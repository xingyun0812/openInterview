package com.openinterview;

import com.openinterview.service.AuditTrailService;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.EvidenceStore;
import com.openinterview.service.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Issue11GateEvidencePackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EvidenceStore evidenceStore;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private EventMappingService eventMappingService;

    @BeforeEach
    void resetState() {
        evidenceStore.clearAll();
        auditTrailService.clear();
        evidenceStore.setRegressionSnapshot(10, 0, List.of());
    }

    private void seedCompleteEventMapping() {
        String trace = "trace-issue11";
        String biz = "BIZ_GATE5_OK";
        for (var e : eventMappingService.mappingTable().entrySet()) {
            evidenceStore.addMq(EventMessage.of(e.getKey(), trace, biz, Map.of()));
            evidenceStore.addWebhook(EventMessage.of(e.getValue(), trace, biz, Map.of()));
        }
    }

    private void seedGoodAudit() {
        AuditTrailService.AuditRecord r = new AuditTrailService.AuditRecord();
        r.module = "test";
        r.action = "sample";
        r.traceId = "trace-audit-ok";
        r.bizCode = "BIZ_OK";
        r.errorCode = "0";
        r.detail = "ok";
        r.occurTime = "2026-03-31 00:00:00";
        auditTrailService.append(r);
    }

    @Test
    void a_gatePack_hasThreeGateBlocksWithStatuses() throws Exception {
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-pack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.gate4.status").exists())
                .andExpect(jsonPath("$.data.gate5.status").exists())
                .andExpect(jsonPath("$.data.gate6.status").exists())
                .andExpect(jsonPath("$.data.gate4.regression").exists())
                .andExpect(jsonPath("$.data.gate5.eventMapping").exists())
                .andExpect(jsonPath("$.data.gate6.traceAudit").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.bizCode").value("GATE_PACK"));
    }

    @Test
    void b_gate5_fail_whenNoEvents_missingListPopulated() throws Exception {
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-pack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gate5.status").value("fail"))
                .andExpect(jsonPath("$.data.gate5.eventMapping.missingEvents").isArray())
                .andExpect(jsonPath("$.data.gate5.eventMapping.missingEvents.length()").value(eventMappingService.expectedMqEventCodes().size()));
    }

    @Test
    void c_gate5_pass_whenFullEventCoverage() throws Exception {
        seedCompleteEventMapping();
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-pack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gate5.status").value("pass"))
                .andExpect(jsonPath("$.data.gate5.eventMapping.coverageRatio").value(1.0))
                .andExpect(jsonPath("$.data.gate5.eventMapping.missingEvents.length()").value(0));
    }

    @Test
    void d_gate6_fail_whenAuditMissingTraceId() throws Exception {
        seedCompleteEventMapping();
        seedGoodAudit();
        AuditTrailService.AuditRecord bad = new AuditTrailService.AuditRecord();
        bad.module = "bad";
        bad.action = "bad";
        bad.traceId = null;
        bad.bizCode = "BIZ_BAD";
        bad.errorCode = "0";
        bad.detail = "missing trace";
        bad.occurTime = "2026-03-31 00:00:01";
        auditTrailService.append(bad);

        mockMvc.perform(get("/api/v1/evidence/gate-pack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gate6.status").value("fail"))
                .andExpect(jsonPath("$.data.gate6.traceAudit.missingFieldSamples").isArray())
                .andExpect(jsonPath("$.data.gate6.traceAudit.missingFieldSamples.length()").value(greaterThan(0)));
    }

    @Test
    void e_gateCheck_releaseReady_whenAllGatesPass() throws Exception {
        seedCompleteEventMapping();
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASE_READY"))
                .andExpect(jsonPath("$.data.failedGates").isArray())
                .andExpect(jsonPath("$.data.failedGates.length()").value(0))
                .andExpect(jsonPath("$.bizCode").value("GATE_CHECK"));
    }

    @Test
    void f_gateCheck_blocked_whenAnyGateFails() throws Exception {
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.failedGates").isArray())
                .andExpect(jsonPath("$.data.failedGates").value(hasItem("gate5")));
    }

    @Test
    void g_evidenceSummaryJsonShape() throws Exception {
        seedCompleteEventMapping();
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-pack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gate4.regression.totalTests").isNumber())
                .andExpect(jsonPath("$.data.gate4.regression.passedTests").isNumber())
                .andExpect(jsonPath("$.data.gate4.regression.failureCount").isNumber())
                .andExpect(jsonPath("$.data.gate4.regression.failures").isArray())
                .andExpect(jsonPath("$.data.gate4.regression.source").isString())
                .andExpect(jsonPath("$.data.gate5.eventMapping.expectedEventCount").isNumber())
                .andExpect(jsonPath("$.data.gate5.eventMapping.coveredCount").isNumber())
                .andExpect(jsonPath("$.data.gate6.traceAudit.traceIdHitRate").isNumber())
                .andExpect(jsonPath("$.data.gate6.traceAudit.bizCodeHitRate").isNumber())
                .andExpect(jsonPath("$.data.gate6.traceAudit.errorCodeHitRate").isNumber());
    }

    @Test
    void h_persist_writesEvidenceJsonFiles() throws Exception {
        seedCompleteEventMapping();
        seedGoodAudit();
        mockMvc.perform(get("/api/v1/evidence/gate-pack").param("persist", "true"))
                .andExpect(status().isOk());

        Path base = Path.of(System.getProperty("user.dir")).resolve("evidence");
        assertTrue(Files.exists(base.resolve("gate4-regression/gate-pack.json")));
        assertTrue(Files.exists(base.resolve("gate5-event-mapping/gate-pack.json")));
        assertTrue(Files.exists(base.resolve("gate6-trace-audit/gate-pack.json")));
    }
}
