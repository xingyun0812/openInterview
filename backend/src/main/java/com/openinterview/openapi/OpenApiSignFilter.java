package com.openinterview.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.common.Result;
import com.openinterview.entity.OpenApiAppEntity;
import com.openinterview.service.IdempotencyService;
import com.openinterview.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class OpenApiSignFilter extends OncePerRequestFilter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenApiAppService openApiAppService;
    private final IdempotencyService idempotencyService;
    private final Duration nonceTtl;
    private final long allowedClockSkewSeconds;

    public OpenApiSignFilter(OpenApiAppService openApiAppService,
                             IdempotencyService idempotencyService,
                             @Value("${openinterview.open-api.nonce-ttl-seconds:300}") long nonceTtlSeconds,
                             @Value("${openinterview.open-api.allowed-skew-seconds:300}") long allowedSkewSeconds) {
        this.openApiAppService = openApiAppService;
        this.idempotencyService = idempotencyService;
        this.nonceTtl = Duration.ofSeconds(Math.max(1, nonceTtlSeconds));
        this.allowedClockSkewSeconds = Math.max(1, allowedSkewSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/open-api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        ReusableBodyRequestWrapper wrapped = new ReusableBodyRequestWrapper(request, body);

        try {
            validate(wrapped, body);
            filterChain.doFilter(wrapped, response);
        } catch (ApiException ex) {
            writeApiError(response, ex);
        }
    }

    private void validate(HttpServletRequest request, byte[] body) {
        String appId = header(request, OpenApiConstants.HEADER_APP_ID);
        String ts = header(request, OpenApiConstants.HEADER_TIMESTAMP);
        String nonce = header(request, OpenApiConstants.HEADER_NONCE);
        String sig = header(request, OpenApiConstants.HEADER_SIGNATURE);

        if (isBlank(appId) || isBlank(ts) || isBlank(nonce) || isBlank(sig)) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "OPEN_API_HEADERS", "缺少开放平台鉴权请求头");
        }

        long epochSeconds = parseEpochSeconds(ts);
        long now = System.currentTimeMillis() / 1000;
        long diff = Math.abs(now - epochSeconds);
        if (diff > allowedClockSkewSeconds) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "OPEN_API_TS", "请求时间戳无效或已过期");
        }

        OpenApiAppEntity app = openApiAppService.requireActiveApp(appId);

        boolean ok = idempotencyService.tryAcquire("openapi:nonce:" + appId + ":" + nonce, nonceTtl);
        if (!ok) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "OPEN_API_NONCE", "Nonce 重复，疑似重放攻击");
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        String rawQuery = request.getQueryString();
        String contentType = request.getContentType();
        String canonical = OpenApiCanonicalizer.canonicalString(method, path, rawQuery, contentType, body, ts, nonce);
        String expected = OpenApiCrypto.hmacSha256Hex(app.appSecret, canonical);

        if (!equalsIgnoreCaseSafe(expected, sig)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "OPEN_API_SIGN", "签名不匹配");
        }

        request.setAttribute(OpenApiConstants.REQ_ATTR_APP_ID, appId);
    }

    private static String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static long parseEpochSeconds(String ts) {
        try {
            return Long.parseLong(ts.trim());
        } catch (Exception e) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "OPEN_API_TS", "时间戳格式错误");
        }
    }

    private static boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b.trim());
    }

    private static void writeApiError(HttpServletResponse response, ApiException ex) throws IOException {
        HttpStatus status;
        if (ex.getErrorCode() == ErrorCode.UNAUTHORIZED) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex.getErrorCode() == ErrorCode.FORBIDDEN) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = TraceContext.getTraceId();
        Result<Void> payload = Result.fail(ex.getErrorCode(), traceId, ex.getBizCode(), ex.getMessage());
        response.getWriter().write(MAPPER.writeValueAsString(payload));
    }
}

