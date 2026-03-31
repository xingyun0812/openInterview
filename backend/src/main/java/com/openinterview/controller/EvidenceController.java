package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.AuditTrailService;
import com.openinterview.service.EvidenceStore;
import com.openinterview.service.GateEvidenceService;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.trace.TraceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class EvidenceController {
    private final EvidenceStore evidenceStore;
    private final AuditTrailService auditTrailService;
    private final InMemoryWorkflowService workflowService;
    private final GateEvidenceService gateEvidenceService;

    public EvidenceController(EvidenceStore evidenceStore,
                              AuditTrailService auditTrailService,
                              InMemoryWorkflowService workflowService,
                              GateEvidenceService gateEvidenceService) {
        this.evidenceStore = evidenceStore;
        this.auditTrailService = auditTrailService;
        this.workflowService = workflowService;
        this.gateEvidenceService = gateEvidenceService;
    }

    @GetMapping("/api/v1/internal/evidence/events")
    public Result<Map<String, Object>> events() {
        Map<String, Object> data = new HashMap<>();
        data.put("mqEvents", evidenceStore.getMqEvents());
        data.put("webhookEvents", evidenceStore.getWebhookEvents());
        data.put("auditLogs", auditTrailService.list());
        data.put("parseFailureAudits", workflowService.getParseFailureAudits());
        data.put("exportAudits", evidenceStore.getExportAudits());
        return Result.success(data, TraceContext.getTraceId(), "EVIDENCE_EVENTS");
    }

    @GetMapping({"/api/v1/evidence/gate-pack", "/api/v1/internal/evidence/gate-pack"})
    public Result<Map<String, Object>> gatePack(@RequestParam(name = "persist", required = false) Boolean persist) throws Exception {
        Map<String, Object> pack = gateEvidenceService.buildGatePack();
        if (Boolean.TRUE.equals(persist)) {
            gateEvidenceService.persistGateEvidenceFiles(pack);
        }
        return Result.success(pack, TraceContext.getTraceId(), "GATE_PACK");
    }

    @GetMapping({"/api/v1/evidence/gate-check", "/api/v1/internal/evidence/gate-check"})
    public Result<Map<String, Object>> gateCheck() {
        Map<String, Object> pack = gateEvidenceService.buildGatePack();
        Map<String, Object> check = gateEvidenceService.buildGateCheck(pack);
        return Result.success(check, TraceContext.getTraceId(), "GATE_CHECK");
    }
}
