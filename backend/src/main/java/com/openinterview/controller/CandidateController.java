package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.AuditTrailService;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/candidate/resume")
public class CandidateController {
    private final InMemoryWorkflowService workflowService;
    private final EventMappingService eventMappingService;
    private final com.openinterview.service.EventBridgeService eventBridgeService;
    private final AuditTrailService auditTrailService;

    public CandidateController(InMemoryWorkflowService workflowService,
                               EventMappingService eventMappingService,
                               com.openinterview.service.EventBridgeService eventBridgeService,
                               AuditTrailService auditTrailService) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/screen")
    public Result<Map<String, Object>> screen(@RequestBody @Validated ScreenRequest request,
                                              @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.ScreenResult result =
                workflowService.createOrGetScreenResult(request.candidateId, request.jobCode, idemKey);
        if (InMemoryWorkflowService.SCREEN_PROCESSING == result.screenStatus) {
            workflowService.markScreenSuccess(request.candidateId, request.jobCode, 82.5, 1);
            result = workflowService.getScreenResult(request.candidateId, request.jobCode);
        }
        String mqEvent = "candidate.resume.screen";
        String webhookEvent = eventMappingService.toWebhookEvent(mqEvent);

        Map<String, Object> data = new HashMap<>();
        data.put("taskCode", result.bizCode);
        data.put("screenStatus", result.screenStatus);
        data.put("reasonSummary", result.reasonSummary);
        data.put("aiSuggestionOnly", true);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, result.bizCode, data);
        auditTrailService.record("candidate", "resume.screen", result.bizCode, "0", "触发简历筛选并进入人工复核闸门");
        return Result.success(data, TraceContext.getTraceId(), result.bizCode);
    }

    @GetMapping("/screen/result/{candidateId}")
    public Result<Map<String, Object>> screenResult(@PathVariable("candidateId") Long candidateId,
                                                    @RequestParam("jobCode") String jobCode) {
        InMemoryWorkflowService.ScreenResult result = workflowService.getScreenResult(candidateId, jobCode);
        Map<String, Object> data = new HashMap<>();
        data.put("candidateId", result.candidateId);
        data.put("jobCode", result.jobCode);
        data.put("screenStatus", result.screenStatus);
        data.put("matchScore", result.matchScore);
        data.put("recommendLevel", result.recommendLevel);
        data.put("reasonSummary", result.reasonSummary);
        data.put("reviewResult", result.reviewResult);
        return Result.success(data, TraceContext.getTraceId(), result.bizCode);
    }

    @PostMapping("/screen/review")
    public Result<Map<String, Object>> screenReview(@RequestBody @Validated ScreenReviewRequest request,
                                                    @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.ScreenResult result = workflowService.reviewScreenResult(
                request.candidateId, request.jobCode, request.reviewResult, request.reviewComment, idemKey
        );
        Map<String, Object> data = new HashMap<>();
        data.put("reviewResult", result.reviewResult);
        data.put("reviewComment", result.reviewComment);
        data.put("reviewTime", result.reviewTime);
        auditTrailService.record("candidate", "resume.screen.review", result.bizCode, "0", "HR完成人工复核");
        return Result.success(data, TraceContext.getTraceId(), result.bizCode);
    }

    public static class ScreenRequest {
        @NotNull
        public Long candidateId;
        @NotBlank
        public String jobCode;
    }

    public static class ScreenReviewRequest {
        @NotNull
        public Long candidateId;
        @NotBlank
        public String jobCode;
        @NotNull
        public Integer reviewResult;
        @NotBlank
        public String reviewComment;
    }
}
