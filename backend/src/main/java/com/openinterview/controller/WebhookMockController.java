package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.trace.TraceContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/webhook/mock", "/api/v1/webhook/mock"})
public class WebhookMockController {

    @PostMapping("/receive")
    public Result<Map<String, Object>> receive(@RequestBody com.openinterview.service.EventMessage eventMessage) {
        return Result.success(Map.of("accepted", true), TraceContext.getTraceId(), eventMessage.bizCode);
    }

    /** 模拟下游返回 500，用于验证 Webhook 重试与失败证据 */
    @PostMapping("/fail")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Map<String, Object>> fail(@RequestBody(required = false) com.openinterview.service.EventMessage eventMessage) {
        return Result.success(Map.of("accepted", false, "reason", "mock failure"), TraceContext.getTraceId(),
                eventMessage != null ? eventMessage.bizCode : "MOCK_FAIL");
    }
}
