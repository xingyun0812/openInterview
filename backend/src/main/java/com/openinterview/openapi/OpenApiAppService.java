package com.openinterview.openapi;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.entity.OpenApiAppEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class OpenApiAppService {
    private final OpenApiAppDbService db;

    public OpenApiAppService(OpenApiAppDbService db) {
        this.db = db;
    }

    public OpenApiAppEntity requireActiveApp(String appId) {
        OpenApiAppEntity app = db.getByAppId(appId);
        if (app == null || app.isDeleted != null && app.isDeleted == 1) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "OPEN_API_APP", "AppId 不存在");
        }
        if (app.status == null || app.status != 1) {
            throw new ApiException(ErrorCode.FORBIDDEN, "OPEN_API_APP", "AppId 已禁用");
        }
        if (app.appSecret == null || app.appSecret.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "OPEN_API_APP", "AppSecret 缺失");
        }
        return app;
    }

    public void requireScope(OpenApiAppEntity app, String scope) {
        if (scope == null || scope.isBlank()) {
            return;
        }
        if (app.apiPermissions == null || app.apiPermissions.isBlank()) {
            return;
        }
        boolean ok = Arrays.stream(app.apiPermissions.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .anyMatch(s -> s.equalsIgnoreCase(scope));
        if (!ok) {
            throw new ApiException(ErrorCode.FORBIDDEN, "OPEN_API_SCOPE", "无权限访问该接口");
        }
    }

    public void updateWebhook(OpenApiAppEntity app, String url, String secret) {
        app.webhookUrl = url;
        app.webhookSecret = secret;
        db.updateById(app);
    }
}

