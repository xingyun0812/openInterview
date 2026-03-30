package com.openinterview;

import com.openinterview.service.EventBridgeService;
import com.openinterview.service.EventMessage;
import com.openinterview.service.EvidenceStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * MQ / Webhook 开关独立：关闭时不发 HTTP，仍写入证据中的 MQ 与「拟投递」Webhook 载荷。
 */
@SpringBootTest(properties = {
        "event.bridge.webhook-enabled=false",
        "event.bridge.mq-enabled=false"
})
class Issue10BridgeToggleTest {

    @Autowired
    private EventBridgeService eventBridgeService;

    @Autowired
    private EvidenceStore evidenceStore;

    @Test
    void mqAndWebhookDisabledStillRecordsPlannedEventsInEvidence() {
        int mq0 = evidenceStore.getMqEvents().size();
        int wh0 = evidenceStore.getWebhookEvents().size();
        eventBridgeService.publish("candidate.resume.screen", "BIZ-TOGGLE", Map.of());
        Assertions.assertEquals(mq0 + 1, evidenceStore.getMqEvents().size());
        Assertions.assertEquals(wh0 + 1, evidenceStore.getWebhookEvents().size());
        EventMessage lastWh = evidenceStore.getWebhookEvents().get(evidenceStore.getWebhookEvents().size() - 1);
        Assertions.assertEquals("CANDIDATE_RESUME_SCREENED", lastWh.eventCode);
    }
}
