package com.openinterview.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventMappingService {
    private static final Map<String, String> MAPPING = Map.of(
            "candidate.resume.parse", "candidate.resume.parsed",
            "candidate.resume.screen", "candidate.resume.screened",
            "interview.assistant.question.generate", "interview.question.generated",
            "interview.assistant.answer.evaluate", "interview.answer.evaluated",
            "export.generate", "export.generated"
    );

    public String toWebhookEvent(String mqEvent) {
        return MAPPING.getOrDefault(mqEvent, mqEvent);
    }
}
