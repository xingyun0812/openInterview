package com.openinterview.config;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IdempotencyHeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if (HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.DELETE.matches(method)) {
            String key = request.getHeader("X-Idempotency-Key");
            if (key == null || key.isBlank()) {
                throw new ApiException(ErrorCode.PARAM_INVALID, "IDEMPOTENCY", "缺少请求头 X-Idempotency-Key");
            }
        }
        return true;
    }
}
