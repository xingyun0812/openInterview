package com.openinterview.controller;

import com.openinterview.common.Result;
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

    public CandidateController(InMemoryWorkflowService workflowService,
                               EventMappingService eventMappingService,
                               com.openinterview.service.EventBridgeService eventBridgeService) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
    }

    @PostMapping("/screen")
    public Result<Map<String, Object>> screen(@RequestBody @Validated ScreenRequest request,
                                              @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.ScreenResult result =
                workflowService.createOrGetScreenResult(request.candidateId, request.jobCode, idemKey);
        workflowService.markScreenSuccess(request.candidateId, request.jobCode, 82.5, 1);
        String mqEvent = "candidate.resume.screen";
        String webhookEvent = eventMappingService.toWebhookEvent(mqEvent);

        Map<String, Object> data = new HashMap<>();
        data.put("taskCode", result.bizCode);
        data.put("screenStatus", InMemoryWorkflowService.SCREEN_SUCCESS);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, result.bizCode, data);
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
