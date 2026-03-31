package com.openinterview.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MQ 内部命令语义 → Webhook 外部事实语义（与契约基线 / 详细设计 6.5 及 Slice-08 验收表一致）。
 */
@Service
public class EventMappingService {
    private static final Map<String, String> MAPPING = new HashMap<>();

    static {
        MAPPING.put("candidate.resume.upload", "CANDIDATE_RESUME_UPLOADED");
        MAPPING.put("candidate.resume.parse", "CANDIDATE_RESUME_PARSED");
        MAPPING.put("candidate.resume.screen", "CANDIDATE_RESUME_SCREENED");
        MAPPING.put("candidate.resume.screen.review", "CANDIDATE_SCREEN_REVIEWED");
        MAPPING.put("interview.assistant.question.generate", "INTERVIEW_QUESTION_GENERATED");
        MAPPING.put("interview.question.review", "INTERVIEW_QUESTION_REVIEWED");
        MAPPING.put("interview.assistant.answer.evaluate", "INTERVIEW_ANSWER_EVALUATED");
        MAPPING.put("export.task.create", "EXPORT_TASK_CREATED");
        MAPPING.put("export.task.complete", "EXPORT_TASK_COMPLETED");
        MAPPING.put("export.task.failed", "EXPORT_TASK_FAILED");
    }

    public String toWebhookEvent(String mqEvent) {
        return MAPPING.getOrDefault(mqEvent, mqEvent);
    }

    public boolean isMapped(String mqEvent) {
        return MAPPING.containsKey(mqEvent);
    }

    public Set<String> expectedMqEventCodes() {
        return Collections.unmodifiableSet(MAPPING.keySet());
    }

    public Map<String, String> mappingTable() {
        return Collections.unmodifiableMap(MAPPING);
    }
}
