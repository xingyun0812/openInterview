package com.openinterview.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.common.ErrorCode;
import com.openinterview.common.Result;
import com.openinterview.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.fail(ErrorCode.UNAUTHORIZED, TraceContext.getTraceId(), "AUTH",
                authException != null && authException.getMessage() != null
                        ? authException.getMessage()
                        : "未认证或登录已过期");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
