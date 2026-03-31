package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.entity.InterviewPlanEntity;
import com.openinterview.entity.InterviewEvaluateEntity;
import com.openinterview.service.EvaluationService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/api/v2/evaluations")
public class EvaluationV2Controller {

    private final EvaluationService evaluationService;

    public EvaluationV2Controller(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/draft")
    public Result<Map<String, Object>> draft(@RequestBody @Valid DraftRequest request) {
        EvaluationService.DraftCommand cmd = new EvaluationService.DraftCommand();
        cmd.interviewId = request.interviewId;
        cmd.interviewerId = request.interviewerId;
        cmd.interviewerName = request.interviewerName;
        cmd.totalScore = request.totalScore;
        cmd.interviewResult = request.interviewResult;
        cmd.advantageComment = request.advantageComment;
        cmd.disadvantageComment = request.disadvantageComment;
        cmd.comprehensiveComment = request.comprehensiveComment;
        cmd.details = request.details == null ? List.of() : request.details.stream().map(d -> {
            EvaluationService.DraftScoreDetail sd = new EvaluationService.DraftScoreDetail();
            sd.itemId = d.itemId;
            sd.itemName = d.itemName;
            sd.itemFullScore = d.itemFullScore;
            sd.score = d.score;
            sd.itemComment = d.itemComment;
            sd.aiScorePreview = d.aiScorePreview;
            return sd;
        }).collect(Collectors.toList());

        InterviewEvaluateEntity saved = evaluationService.draft(cmd);
        Map<String, Object> data = new HashMap<>();
        data.put("evaluateId", saved.id);
        data.put("interviewId", saved.interviewId);
        data.put("interviewerId", saved.interviewerId);
        data.put("interviewerName", saved.interviewerName);
        data.put("totalScore", saved.totalScore);
        data.put("interviewResult", saved.interviewResult);
        data.put("submitStatus", saved.submitStatus);
        data.put("submitTime", saved.submitTime);
        data.put("aiSuggestionOnly", true);
        return Result.success(data, TraceContext.getTraceId(), "EVAL_DRAFT");
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody @Valid SubmitRequest request) {
        EvaluationService.SubmitCommand cmd = new EvaluationService.SubmitCommand();
        cmd.interviewId = request.interviewId;
        cmd.interviewerId = request.interviewerId;
        InterviewEvaluateEntity saved = evaluationService.submit(cmd);
        Map<String, Object> data = new HashMap<>();
        data.put("evaluateId", saved.id);
        data.put("interviewId", saved.interviewId);
        data.put("interviewerId", saved.interviewerId);
        data.put("submitStatus", saved.submitStatus);
        data.put("submitTime", saved.submitTime);
        return Result.success(data, TraceContext.getTraceId(), "EVAL_SUBMIT");
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam("interviewId") Long interviewId) {
        List<EvaluationService.EvaluationView> evals = evaluationService.listEvaluations(interviewId);
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);
        data.put("evaluations", evals);
        data.put("aiSuggestionOnly", true);
        return Result.success(data, TraceContext.getTraceId(), "EVAL_LIST");
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam("interviewId") Long interviewId) {
        EvaluationService.SummaryView s = evaluationService.summarize(interviewId);
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", interviewId);
        data.put("finalScore", s.finalScore);
        data.put("humanAvgScore", s.humanAvgScore);
        data.put("submittedEvaluateCount", s.submittedEvaluateCount);
        data.put("totalEvaluateCount", s.totalEvaluateCount);
        data.put("flowStatus", s.flowStatus);
        data.put("aiSuggestionOnly", true);
        return Result.success(data, TraceContext.getTraceId(), "EVAL_SUMMARY");
    }

    @PostMapping("/review")
    public Result<Map<String, Object>> review(@RequestBody @Valid ReviewRequest request) {
        EvaluationService.ReviewCommand cmd = new EvaluationService.ReviewCommand();
        cmd.interviewId = request.interviewId;
        cmd.interviewResult = request.interviewResult;
        cmd.remark = request.remark;
        InterviewPlanEntity plan = evaluationService.review(cmd);
        Map<String, Object> data = new HashMap<>();
        data.put("interviewId", plan.id);
        data.put("finalScore", plan.finalScore);
        data.put("interviewResult", plan.interviewResult);
        data.put("remark", plan.remark);
        data.put("flowStatus", "REVIEWED");
        return Result.success(data, TraceContext.getTraceId(), "EVAL_REVIEW");
    }

    public static class DraftRequest {
        @NotNull
        public Long interviewId;
        @NotNull
        public Long interviewerId;
        @NotBlank
        public String interviewerName;
        @NotNull
        public BigDecimal totalScore;
        @NotNull
        public Integer interviewResult;
        public String advantageComment;
        public String disadvantageComment;
        @NotBlank
        public String comprehensiveComment;
        @NotNull
        public List<DraftDetail> details;
    }

    public static class DraftDetail {
        @NotNull
        public Long itemId;
        @NotBlank
        public String itemName;
        @NotNull
        public Integer itemFullScore;
        @NotNull
        public BigDecimal score;
        public String itemComment;
        public BigDecimal aiScorePreview;
    }

    public static class SubmitRequest {
        @NotNull
        public Long interviewId;
        @NotNull
        public Long interviewerId;
    }

    public static class ReviewRequest {
        @NotNull
        public Long interviewId;
        @NotNull
        public Integer interviewResult;
        public String remark;
    }
}

