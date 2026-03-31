package com.openinterview.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static com.openinterview.openapi.OpenApiConstants.HEADER_APP_ID;
import static com.openinterview.openapi.OpenApiConstants.HEADER_NONCE;
import static com.openinterview.openapi.OpenApiConstants.HEADER_SIGNATURE;
import static com.openinterview.openapi.OpenApiConstants.HEADER_TIMESTAMP;

public final class OpenApiTestSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenApiTestSupport() {
    }

    public static MockHttpServletRequestBuilder sign(MockHttpServletRequestBuilder b,
                                                     String appId,
                                                     String appSecret,
                                                     String method,
                                                     String path,
                                                     String rawQuery,
                                                     String contentType,
                                                     byte[] body,
                                                     long epochSeconds,
                                                     String nonce) {
        String ts = String.valueOf(epochSeconds);
        String canonical = OpenApiCanonicalizer.canonicalString(method, path, rawQuery, contentType, body, ts, nonce);
        String sig = OpenApiCrypto.hmacSha256Hex(appSecret, canonical);
        return b.header(HEADER_APP_ID, appId)
                .header(HEADER_TIMESTAMP, ts)
                .header(HEADER_NONCE, nonce)
                .header(HEADER_SIGNATURE, sig);
    }

    public static byte[] jsonBody(Map<String, Object> body) {
        try {
            return MAPPER.writeValueAsBytes(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static MediaType jsonUtf8() {
        return new MediaType("application", "json", StandardCharsets.UTF_8);
    }

    public static long nowEpochSeconds() {
        return Instant.now().getEpochSecond();
    }
}

