package com.openinterview.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ResumeParseAsyncLoopService {
    private final InMemoryWorkflowService workflowService;
    private final EventBridgeService eventBridgeService;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private final long[] backoffMs;

    public ResumeParseAsyncLoopService(InMemoryWorkflowService workflowService,
                                       EventBridgeService eventBridgeService,
                                       @Value("${resume.parse.retry.backoff-ms:2000,5000,10000}") String backoffText) {
        this.workflowService = workflowService;
        this.eventBridgeService = eventBridgeService;
        this.backoffMs = parseBackoff(backoffText);
    }

    public void submitParseTask(String taskCode) {
        scheduleAttempt(taskCode, 0L);
    }

    private void scheduleAttempt(String taskCode, long delayMs) {
        executorService.schedule(() -> consumeParseTask(taskCode), delayMs, TimeUnit.MILLISECONDS);
    }

    private void consumeParseTask(String taskCode) {
        InMemoryWorkflowService.ParseAttemptResult attempt = workflowService.executeParseAttempt(taskCode);
        if (attempt.success) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("candidateId", attempt.candidateId);
            payload.put("taskCode", attempt.taskCode);
            payload.put("parseStatus", InMemoryWorkflowService.PARSE_SUCCESS);
            eventBridgeService.publish("candidate.resume.parse", attempt.bizCode, payload, attempt.traceId);
            return;
        }

        workflowService.addParseFailureAudit(
                attempt.traceId,
                attempt.bizCode,
                attempt.errorCode,
                attempt.failReason,
                attempt.currentAttempt,
                attempt.exhausted
        );

        if (!attempt.exhausted) {
            long delayMs = backoffMs[Math.min(attempt.currentAttempt - 1, backoffMs.length - 1)];
            scheduleAttempt(taskCode, delayMs);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("candidateId", attempt.candidateId);
        payload.put("taskCode", attempt.taskCode);
        payload.put("parseStatus", InMemoryWorkflowService.PARSE_FAILED);
        payload.put("errorCode", attempt.errorCode);
        payload.put("failReason", attempt.failReason);
        eventBridgeService.publish("candidate.resume.parse", attempt.bizCode, payload, attempt.traceId);
    }

    private long[] parseBackoff(String backoffText) {
        String[] split = backoffText.split(",");
        long[] values = new long[Math.max(1, split.length)];
        for (int i = 0; i < split.length; i++) {
            values[i] = Long.parseLong(split[i].trim());
        }
        if (values.length == 0) {
            return new long[]{2000L, 5000L, 10000L};
        }
        return values;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}
