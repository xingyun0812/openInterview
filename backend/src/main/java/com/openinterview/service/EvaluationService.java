package com.openinterview.service;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.entity.InterviewEvaluateEntity;
import com.openinterview.entity.InterviewPlanEntity;
import com.openinterview.entity.ScoreDetailEntity;
import com.openinterview.service.db.InterviewEvaluateDbService;
import com.openinterview.service.db.InterviewPlanDbService;
import com.openinterview.service.db.ScoreDetailDbService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    public static final int SUBMIT_STATUS_DRAFT = 0;
    public static final int SUBMIT_STATUS_SUBMITTED = 1;

    private final InterviewPlanDbService interviewPlanDbService;
    private final InterviewEvaluateDbService interviewEvaluateDbService;
    private final ScoreDetailDbService scoreDetailDbService;

    public EvaluationService(InterviewPlanDbService interviewPlanDbService,
                             InterviewEvaluateDbService interviewEvaluateDbService,
                             ScoreDetailDbService scoreDetailDbService) {
        this.interviewPlanDbService = interviewPlanDbService;
        this.interviewEvaluateDbService = interviewEvaluateDbService;
        this.scoreDetailDbService = scoreDetailDbService;
    }

    @Transactional
    public InterviewEvaluateEntity draft(DraftCommand cmd) {
        InterviewPlanEntity plan = requirePlan(cmd.interviewId);
        InterviewEvaluateEntity existing = interviewEvaluateDbService.getByInterviewAndInterviewer(
                cmd.interviewId, cmd.interviewerId);
        if (existing != null && Objects.equals(existing.submitStatus, SUBMIT_STATUS_SUBMITTED)) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "当前评价已提交，不能再暂存");
        }

        InterviewEvaluateEntity e = existing != null ? existing : new InterviewEvaluateEntity();
        e.interviewId = cmd.interviewId;
        e.interviewerId = cmd.interviewerId;
        e.interviewerName = cmd.interviewerName;
        e.totalScore = cmd.totalScore;
        e.interviewResult = cmd.interviewResult;
        e.advantageComment = cmd.advantageComment;
        e.disadvantageComment = cmd.disadvantageComment;
        e.comprehensiveComment = cmd.comprehensiveComment;
        e.submitStatus = SUBMIT_STATUS_DRAFT;
        e.submitTime = null;
        e.isSigned = e.isSigned == null ? 0 : e.isSigned;
        e.isDeleted = e.isDeleted == null ? 0 : e.isDeleted;

        if (existing == null) {
            interviewEvaluateDbService.create(e);
        } else {
            interviewEvaluateDbService.updateById(e);
        }

        scoreDetailDbService.deleteByEvaluateId(e.id);
        if (cmd.details != null) {
            for (DraftScoreDetail d : cmd.details) {
                ScoreDetailEntity sd = new ScoreDetailEntity();
                sd.evaluateId = e.id;
                sd.interviewId = cmd.interviewId;
                sd.itemId = d.itemId;
                sd.itemName = d.itemName;
                sd.itemFullScore = d.itemFullScore;
                sd.score = d.score;
                sd.itemComment = d.itemComment;
                sd.isDeleted = 0;
                scoreDetailDbService.create(sd);
            }
        }
        return interviewEvaluateDbService.getById(e.id);
    }

    @Transactional
    public InterviewEvaluateEntity submit(SubmitCommand cmd) {
        InterviewPlanEntity plan = requirePlan(cmd.interviewId);
        InterviewEvaluateEntity e = interviewEvaluateDbService.getByInterviewAndInterviewer(cmd.interviewId, cmd.interviewerId);
        if (e == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "请先暂存评价再提交");
        }
        if (Objects.equals(e.submitStatus, SUBMIT_STATUS_SUBMITTED)) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "当前评价已提交");
        }
        List<ScoreDetailEntity> details = scoreDetailDbService.listByEvaluateId(e.id);
        if (details == null || details.isEmpty()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "评分明细不能为空");
        }
        e.submitStatus = SUBMIT_STATUS_SUBMITTED;
        e.submitTime = LocalDateTime.now();
        interviewEvaluateDbService.updateById(e);
        return interviewEvaluateDbService.getById(e.id);
    }

    public List<EvaluationView> listEvaluations(Long interviewId) {
        InterviewPlanEntity plan = requirePlan(interviewId);
        List<InterviewEvaluateEntity> evals = interviewEvaluateDbService.listByInterviewId(interviewId);
        evals.sort(Comparator.comparingLong(o -> o.id == null ? 0L : o.id));
        Map<Long, List<ScoreDetailEntity>> detailByEvalId = new HashMap<>();
        for (InterviewEvaluateEntity e : evals) {
            detailByEvalId.put(e.id, scoreDetailDbService.listByEvaluateId(e.id));
        }
        List<EvaluationView> out = new ArrayList<>();
        for (InterviewEvaluateEntity e : evals) {
            EvaluationView v = new EvaluationView();
            v.interviewId = interviewId;
            v.interviewCode = plan.interviewCode;
            v.evaluateId = e.id;
            v.interviewerId = e.interviewerId;
            v.interviewerName = e.interviewerName;
            v.totalScore = e.totalScore;
            v.interviewResult = e.interviewResult;
            v.advantageComment = e.advantageComment;
            v.disadvantageComment = e.disadvantageComment;
            v.comprehensiveComment = e.comprehensiveComment;
            v.submitStatus = e.submitStatus;
            v.submitTime = e.submitTime;
            v.details = detailByEvalId.getOrDefault(e.id, List.of()).stream().map(sd -> {
                ScoreDetailView dv = new ScoreDetailView();
                dv.itemId = sd.itemId;
                dv.itemName = sd.itemName;
                dv.itemFullScore = sd.itemFullScore;
                dv.score = sd.score;
                dv.itemComment = sd.itemComment;
                return dv;
            }).collect(Collectors.toList());
            v.flowStatus = flowStatus(plan, e);
            out.add(v);
        }
        return out;
    }

    @Transactional
    public SummaryView summarize(Long interviewId) {
        InterviewPlanEntity plan = requirePlan(interviewId);
        if (plan.finalScore != null) {
            return buildSummary(plan, interviewId);
        }
        List<InterviewEvaluateEntity> evals = interviewEvaluateDbService.listByInterviewId(interviewId);
        if (evals == null || evals.isEmpty()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "未找到任何面试官评价，无法汇总");
        }
        boolean allSubmitted = evals.stream().allMatch(e -> Objects.equals(e.submitStatus, SUBMIT_STATUS_SUBMITTED));
        if (!allSubmitted) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "存在未提交评价，不能汇总");
        }
        BigDecimal avg = avgTotalScore(evals);
        plan.finalScore = avg;
        interviewPlanDbService.updateById(plan);
        return buildSummary(interviewPlanDbService.getById(plan.id), interviewId);
    }

    @Transactional
    public InterviewPlanEntity review(ReviewCommand cmd) {
        InterviewPlanEntity plan = requirePlan(cmd.interviewId);
        if (plan.finalScore == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "请先完成汇总再复核");
        }
        if (plan.interviewResult != null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, plan.interviewCode, "当前面试已复核，不能重复复核");
        }
        plan.interviewResult = cmd.interviewResult;
        if (cmd.remark != null) {
            plan.remark = cmd.remark;
        }
        interviewPlanDbService.updateById(plan);
        return interviewPlanDbService.getById(plan.id);
    }

    private SummaryView buildSummary(InterviewPlanEntity plan, Long interviewId) {
        List<InterviewEvaluateEntity> evals = interviewEvaluateDbService.listByInterviewId(interviewId);
        BigDecimal avg = evals == null || evals.isEmpty() ? null : avgTotalScore(evals.stream()
                .filter(e -> Objects.equals(e.submitStatus, SUBMIT_STATUS_SUBMITTED))
                .collect(Collectors.toList()));
        SummaryView s = new SummaryView();
        s.interviewId = interviewId;
        s.interviewCode = plan.interviewCode;
        s.finalScore = plan.finalScore;
        s.submittedEvaluateCount = (int) (evals == null ? 0 : evals.stream()
                .filter(e -> Objects.equals(e.submitStatus, SUBMIT_STATUS_SUBMITTED)).count());
        s.totalEvaluateCount = evals == null ? 0 : evals.size();
        s.humanAvgScore = avg;
        s.flowStatus = flowStatus(plan, null);
        s.aiSuggestionOnly = true;
        return s;
    }

    private String flowStatus(InterviewPlanEntity plan, InterviewEvaluateEntity anyEvaluate) {
        if (anyEvaluate != null && !Objects.equals(anyEvaluate.submitStatus, SUBMIT_STATUS_SUBMITTED)) {
            return "DRAFT";
        }
        if (anyEvaluate != null && Objects.equals(anyEvaluate.submitStatus, SUBMIT_STATUS_SUBMITTED)
                && plan.finalScore == null) {
            return "SUBMITTED";
        }
        if (plan.finalScore != null && plan.interviewResult == null) {
            return "SUMMARIZED";
        }
        if (plan.finalScore != null && plan.interviewResult != null) {
            return "REVIEWED";
        }
        return "DRAFT";
    }

    private BigDecimal avgTotalScore(List<InterviewEvaluateEntity> evals) {
        if (evals == null || evals.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (InterviewEvaluateEntity e : evals) {
            if (e.totalScore == null) {
                continue;
            }
            sum = sum.add(e.totalScore);
            n++;
        }
        if (n == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    private InterviewPlanEntity requirePlan(Long interviewId) {
        InterviewPlanEntity plan = interviewPlanDbService.getById(interviewId);
        if (plan == null) {
            throw new ApiException(ErrorCode.INTERVIEW_NOT_FOUND, "INT_PLAN_NONE", "面试计划不存在");
        }
        return plan;
    }

    public static class DraftCommand {
        public Long interviewId;
        public Long interviewerId;
        public String interviewerName;
        public BigDecimal totalScore;
        public Integer interviewResult;
        public String advantageComment;
        public String disadvantageComment;
        public String comprehensiveComment;
        public List<DraftScoreDetail> details;
    }

    public static class DraftScoreDetail {
        public Long itemId;
        public String itemName;
        public Integer itemFullScore;
        public BigDecimal score;
        public String itemComment;
        public BigDecimal aiScorePreview;
    }

    public static class SubmitCommand {
        public Long interviewId;
        public Long interviewerId;
    }

    public static class ReviewCommand {
        public Long interviewId;
        public Integer interviewResult;
        public String remark;
    }

    public static class EvaluationView {
        public Long interviewId;
        public String interviewCode;
        public Long evaluateId;
        public Long interviewerId;
        public String interviewerName;
        public BigDecimal totalScore;
        public Integer interviewResult;
        public String advantageComment;
        public String disadvantageComment;
        public String comprehensiveComment;
        public Integer submitStatus;
        public LocalDateTime submitTime;
        public List<ScoreDetailView> details;
        public String flowStatus;
        public boolean aiSuggestionOnly = true;
    }

    public static class ScoreDetailView {
        public Long itemId;
        public String itemName;
        public Integer itemFullScore;
        public BigDecimal score;
        public String itemComment;
    }

    public static class SummaryView {
        public Long interviewId;
        public String interviewCode;
        public BigDecimal finalScore;
        public BigDecimal humanAvgScore;
        public int submittedEvaluateCount;
        public int totalEvaluateCount;
        public String flowStatus;
        public boolean aiSuggestionOnly;
    }
}

