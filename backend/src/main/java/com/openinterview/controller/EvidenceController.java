package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.AuditTrailService;
import com.openinterview.service.EvidenceStore;
import com.openinterview.trace.TraceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/evidence")
public class EvidenceController {
    private final EvidenceStore evidenceStore;
    private final AuditTrailService auditTrailService;

    public EvidenceController(EvidenceStore evidenceStore, AuditTrailService auditTrailService) {
        this.evidenceStore = evidenceStore;
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/events")
    public Result<Map<String, Object>> events() {
        Map<String, Object> data = new HashMap<>();
        data.put("mqEvents", evidenceStore.getMqEvents());
        data.put("webhookEvents", evidenceStore.getWebhookEvents());
        data.put("auditLogs", auditTrailService.list());
        return Result.success(data, TraceContext.getTraceId(), "EVIDENCE_EVENTS");
    }
}
