package com.openinterview.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
public class EventMappingService {
    private static final Map<String, String> MAPPING = Map.of(
            "candidate.resume.parse", "candidate.resume.parsed",
            "candidate.resume.screen", "candidate.resume.screened",
            "interview.assistant.question.generate", "interview.question.generated",
            "interview.answer.evaluate", "interview.answer.evaluated",
            "export.generate", "export.generated"
    );

    public String toWebhookEvent(String mqEvent) {
        return MAPPING.getOrDefault(mqEvent, mqEvent);
    }

    /** MQ 侧需要覆盖的事件编码（与 MAPPING 键一致）。 */
    public Set<String> expectedMqEventCodes() {
        return Collections.unmodifiableSet(MAPPING.keySet());
    }

    public Map<String, String> mappingTable() {
        return Collections.unmodifiableMap(MAPPING);
    }
}
