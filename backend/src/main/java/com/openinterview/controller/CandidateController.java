package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.AuditTrailService;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.service.ResumeParseAsyncLoopService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
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
    private final ResumeParseAsyncLoopService resumeParseAsyncLoopService;
    private final AuditTrailService auditTrailService;

    public CandidateController(InMemoryWorkflowService workflowService,
                               EventMappingService eventMappingService,
                               com.openinterview.service.EventBridgeService eventBridgeService,
                               ResumeParseAsyncLoopService resumeParseAsyncLoopService,
                               AuditTrailService auditTrailService) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
        this.resumeParseAsyncLoopService = resumeParseAsyncLoopService;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("candidateId") Long candidateId,
                                              @RequestParam("resumeFile") MultipartFile resumeFile,
                                              @RequestHeader("X-Idempotency-Key") String idemKey) throws Exception {
        InMemoryWorkflowService.UploadResult result = workflowService.uploadResume(
                candidateId,
                resumeFile.getOriginalFilename(),
                resumeFile.getBytes(),
                idemKey
        );
        Map<String, Object> data = new HashMap<>();
        data.put("candidateId", result.candidateId);
        data.put("resumeUrl", result.resumeUrl);
        String mqEvent = "candidate.resume.upload";
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", eventMappingService.toWebhookEvent(mqEvent));
        eventBridgeService.publish(mqEvent, result.bizCode, data);
        auditTrailService.record("candidate", "resume.upload", result.bizCode, "0", "简历上传至对象存储(mock)");
        return Result.success(data, TraceContext.getTraceId(), result.bizCode);
    }

    @PostMapping("/parse")
    public Result<Map<String, Object>> parse(@RequestBody @Validated ParseRequest request,
                                             @RequestHeader("X-Idempotency-Key") String idemKey) {
        String traceId = TraceContext.getTraceId();
        InMemoryWorkflowService.ParseTask task = workflowService.createOrGetParseTask(
                request.candidateId, request.resumeUrl, idemKey, traceId
        );
        resumeParseAsyncLoopService.submitParseTask(task.taskCode);
        String mqEvent = "candidate.resume.parse";
        String webhookEvent = eventMappingService.toWebhookEvent(mqEvent);

        Map<String, Object> data = new HashMap<>();
        data.put("taskCode", task.taskCode);
        data.put("parseStatus", task.parseStatus);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, task.bizCode, data);
        auditTrailService.record("candidate", "resume.parse", task.bizCode, "0", "触发异步解析任务");
        return Result.success(data, traceId, task.bizCode);
    }

    @GetMapping("/parse/result/{candidateId}")
    public Result<Map<String, Object>> parseResult(@PathVariable("candidateId") Long candidateId) {
        InMemoryWorkflowService.ResumeParseResult result = workflowService.getParseResult(candidateId);
        Map<String, Object> data = new HashMap<>();
        data.put("candidateId", result.candidateId);
        data.put("parseStatus", result.parseStatus);
        data.put("basicInfo", result.basicInfo);
        data.put("education", result.education);
        data.put("workExperience", result.workExperience);
        data.put("skillTags", result.skillTags);
        data.put("errorCode", result.errorCode);
        data.put("failReason", result.failReason);
        return Result.success(data, TraceContext.getTraceId(), result.bizCode);
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
        String mqEvent = "candidate.resume.screen.review";
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", eventMappingService.toWebhookEvent(mqEvent));
        eventBridgeService.publish(mqEvent, result.bizCode, data);
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

    public static class ParseRequest {
        @NotNull
        public Long candidateId;
        @NotBlank
        public String resumeUrl;
    }
}
