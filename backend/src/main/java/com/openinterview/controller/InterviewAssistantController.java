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
@RequestMapping("/api/v1/interview/assistant")
public class InterviewAssistantController {
    private final InMemoryWorkflowService workflowService;
    private final EventMappingService eventMappingService;
    private final com.openinterview.service.EventBridgeService eventBridgeService;

    public InterviewAssistantController(InMemoryWorkflowService workflowService,
                                        EventMappingService eventMappingService,
                                        com.openinterview.service.EventBridgeService eventBridgeService) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
    }

    @PostMapping("/question/generate")
    public Result<Map<String, Object>> questionGenerate(@RequestBody @Validated QuestionGenerateRequest request,
                                                        @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.QuestionRecord record = workflowService.createQuestionRecord(
                request.interviewId, request.resumeSectionId, request.difficultyLevel, request.questionCount, idemKey
        );
        String mqEvent = "interview.assistant.question.generate";
        String webhookEvent = eventMappingService.toWebhookEvent(mqEvent);
        Map<String, Object> data = new HashMap<>();
        data.put("requestCode", record.requestCode);
        data.put("reviewStatus", record.reviewStatus);
        data.put("questionCount", record.questionCount);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, record.bizCode, data);
        return Result.success(data, TraceContext.getTraceId(), record.bizCode);
    }

    @PostMapping("/question/review")
    public Result<Map<String, Object>> questionReview(@RequestBody @Validated QuestionReviewRequest request,
                                                      @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.QuestionRecord record = workflowService.reviewQuestion(
                request.requestCode, request.reviewStatus, request.reviewComment, idemKey
        );
        Map<String, Object> data = new HashMap<>();
        data.put("requestCode", record.requestCode);
        data.put("reviewStatus", record.reviewStatus);
        data.put("reviewComment", record.reviewComment);
        return Result.success(data, TraceContext.getTraceId(), record.bizCode);
    }

    @PostMapping("/answer/evaluate")
    public Result<Map<String, Object>> answerEvaluate(@RequestBody @Validated AnswerEvaluateRequest request,
                                                      @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.AnswerEvaluateResult result = workflowService.evaluateAnswer(
                request.interviewId, request.questionId, request.answerText, idemKey
        );
        String mqEvent = "interview.assistant.answer.evaluate";
        String webhookEvent = eventMappingService.toWebhookEvent(mqEvent);
        Map<String, Object> data = new HashMap<>();
        data.put("accuracyScore", result.accuracyScore);
        data.put("coverageScore", result.coverageScore);
        data.put("clarityScore", result.clarityScore);
        data.put("followUpSuggest", result.followUpSuggest);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, result.bizCode, data);
        return Result.success(data, TraceContext.getTraceId(), result.bizCode);
    }

    public static class QuestionGenerateRequest {
        @NotNull
        public Long interviewId;
        @NotBlank
        public String resumeSectionId;
        @NotNull
        public Integer difficultyLevel;
        @NotNull
        public Integer questionCount;
    }

    public static class QuestionReviewRequest {
        @NotBlank
        public String requestCode;
        @NotNull
        public Integer reviewStatus;
        @NotBlank
        public String reviewComment;
    }

    public static class AnswerEvaluateRequest {
        @NotNull
        public Long interviewId;
        @NotNull
        public Long questionId;
        @NotBlank
        public String answerText;
    }
}
