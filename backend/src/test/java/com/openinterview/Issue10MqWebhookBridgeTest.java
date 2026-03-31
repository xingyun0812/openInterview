package com.openinterview;

import com.openinterview.service.EventBridgeService;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.EventMessage;
import com.openinterview.service.EvidenceStore;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-08：MQ→Webhook 映射、证据链、Webhook 重试与失败追溯。
 */
@SpringBootTest
@AutoConfigureMockMvc
class Issue10MqWebhookBridgeTest {

    static final MockWebServer MOCK_WEB_SERVER;

    static {
        MOCK_WEB_SERVER = new MockWebServer();
        try {
            MOCK_WEB_SERVER.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void bridgeProps(DynamicPropertyRegistry registry) {
        registry.add("event.bridge.webhook-enabled", () -> "true");
        registry.add("event.bridge.mq-enabled", () -> "false");
        registry.add("event.bridge.webhook-url", () -> MOCK_WEB_SERVER.url("/receive").toString());
    }

    @AfterAll
    static void shutdownMock() throws Exception {
        MOCK_WEB_SERVER.shutdown();
    }

    @Autowired
    private EventMappingService eventMappingService;

    @Autowired
    private EventBridgeService eventBridgeService;

    @Autowired
    private EvidenceStore evidenceStore;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void a_allTenMappingsMatchBaseline() {
        Assertions.assertEquals("CANDIDATE_RESUME_UPLOADED", eventMappingService.toWebhookEvent("candidate.resume.upload"));
        Assertions.assertEquals("CANDIDATE_RESUME_PARSED", eventMappingService.toWebhookEvent("candidate.resume.parse"));
        Assertions.assertEquals("CANDIDATE_RESUME_SCREENED", eventMappingService.toWebhookEvent("candidate.resume.screen"));
        Assertions.assertEquals("CANDIDATE_SCREEN_REVIEWED", eventMappingService.toWebhookEvent("candidate.resume.screen.review"));
        Assertions.assertEquals("INTERVIEW_QUESTION_GENERATED", eventMappingService.toWebhookEvent("interview.assistant.question.generate"));
        Assertions.assertEquals("INTERVIEW_QUESTION_REVIEWED", eventMappingService.toWebhookEvent("interview.question.review"));
        Assertions.assertEquals("INTERVIEW_ANSWER_EVALUATED", eventMappingService.toWebhookEvent("interview.assistant.answer.evaluate"));
        Assertions.assertEquals("EXPORT_TASK_CREATED", eventMappingService.toWebhookEvent("export.task.create"));
        Assertions.assertEquals("EXPORT_TASK_COMPLETED", eventMappingService.toWebhookEvent("export.task.complete"));
        Assertions.assertEquals("EXPORT_TASK_FAILED", eventMappingService.toWebhookEvent("export.task.failed"));
    }

    @Test
    void b_publishIncreasesMqAndWebhookEvidence() {
        int mqBefore = evidenceStore.getMqEvents().size();
        int whBefore = evidenceStore.getWebhookEvents().size();
        MOCK_WEB_SERVER.enqueue(new MockResponse().setResponseCode(200));
        eventBridgeService.publish("candidate.resume.upload", "BIZ-I10-B", Map.of("k", "v"));
        Assertions.assertEquals(mqBefore + 1, evidenceStore.getMqEvents().size());
        Assertions.assertEquals(whBefore + 1, evidenceStore.getWebhookEvents().size());
    }

    @Test
    void c_webhookCallbackSuccess() throws Exception {
        long reqBefore = MOCK_WEB_SERVER.getRequestCount();
        MOCK_WEB_SERVER.enqueue(new MockResponse().setResponseCode(200));
        eventBridgeService.publish("export.task.create", "BIZ-I10-C", Map.of("taskId", 1L));
        Assertions.assertEquals(reqBefore + 1, MOCK_WEB_SERVER.getRequestCount());
    }

    @Test
    void d_webhookFailureThenRetryThenFailureEvidence() {
        int failBefore = evidenceStore.getWebhookDeliveryFailures().size();
        MOCK_WEB_SERVER.enqueue(new MockResponse().setResponseCode(500));
        MOCK_WEB_SERVER.enqueue(new MockResponse().setResponseCode(500));
        eventBridgeService.publish("candidate.resume.parse", "BIZ-I10-D", Map.of(), "trace-i10-d");
        Assertions.assertEquals(failBefore + 1, evidenceStore.getWebhookDeliveryFailures().size());
        Map<String, Object> last = evidenceStore.getWebhookDeliveryFailures().get(evidenceStore.getWebhookDeliveryFailures().size() - 1);
        Assertions.assertEquals("BIZ-I10-D", last.get("bizCode"));
        Assertions.assertEquals("candidate.resume.parse", last.get("mqEventCode"));
        Assertions.assertEquals("CANDIDATE_RESUME_PARSED", last.get("webhookEventCode"));
        Assertions.assertEquals("trace-i10-d", last.get("traceId"));
    }

    @Test
    void f_unmappedMqEventPassesThroughAsWebhookName() {
        Assertions.assertEquals("custom.unknown.event", eventMappingService.toWebhookEvent("custom.unknown.event"));
        Assertions.assertFalse(eventMappingService.isMapped("custom.unknown.event"));
    }

    @Test
    void g_traceIdAndBizCodePropagatedInEventMessage() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("x", 1);
        MOCK_WEB_SERVER.enqueue(new MockResponse().setResponseCode(200));
        eventBridgeService.publish("interview.assistant.answer.evaluate", "BIZ-TRACE", payload, "trace-issue10-g");
        EventMessage mq = evidenceStore.getMqEvents().get(evidenceStore.getMqEvents().size() - 1);
        EventMessage wh = evidenceStore.getWebhookEvents().get(evidenceStore.getWebhookEvents().size() - 1);
        Assertions.assertEquals("trace-issue10-g", mq.traceId);
        Assertions.assertEquals("BIZ-TRACE", mq.bizCode);
        Assertions.assertEquals("trace-issue10-g", wh.traceId);
        Assertions.assertEquals("BIZ-TRACE", wh.bizCode);
    }

    @Test
    void h_evidenceEndpointExposesMqAndWebhookArrays() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mqEvents").isArray())
                .andExpect(jsonPath("$.data.webhookEvents").isArray())
                .andExpect(jsonPath("$.data.webhookDeliveryFailures").isArray());
    }
}
