package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.EvidenceStore;
import com.openinterview.service.EventMessage;
import com.openinterview.trace.TraceContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook/mock")
public class WebhookMockController {
    private final EvidenceStore evidenceStore;

    public WebhookMockController(EvidenceStore evidenceStore) {
        this.evidenceStore = evidenceStore;
    }

    @PostMapping("/receive")
    public Result<Map<String, Object>> receive(@RequestBody EventMessage eventMessage) {
        evidenceStore.addWebhook(eventMessage);
        return Result.success(Map.of("accepted", true), TraceContext.getTraceId(), eventMessage.bizCode);
    }
}
