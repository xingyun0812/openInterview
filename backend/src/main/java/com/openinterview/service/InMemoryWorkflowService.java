package com.openinterview.service;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class InMemoryWorkflowService {
    public static final int SCREEN_PROCESSING = 1;
    public static final int SCREEN_SUCCESS = 2;
    public static final int SCREEN_FAILED = 3;
    public static final int SCREEN_CANCELED = 4;

    public static final int REVIEW_PENDING = 1;
    public static final int REVIEW_APPROVED = 2;
    public static final int REVIEW_REJECTED = 3;

    public static final int EXPORT_SCREENING_EXCEL = 0;
    public static final int EXPORT_INTERVIEW_EXCEL = 1;
    public static final int EXPORT_INTERVIEW_WORD = 2;

    public static final int TASK_PROCESSING = 1;
    public static final int TASK_SUCCESS = 2;
    public static final int TASK_FAILED = 3;

    private final Map<String, Object> idemCache = new ConcurrentHashMap<>();
    private final Map<String, ScreenResult> screenStore = new ConcurrentHashMap<>();
    private final Map<String, QuestionRecord> questionStore = new ConcurrentHashMap<>();
    private final Map<String, ExportTask> exportTaskStore = new ConcurrentHashMap<>();

    public <T> T idempotent(String key, Supplier<T> supplier) {
        Object old = idemCache.get(key);
        if (old != null) {
            @SuppressWarnings("unchecked")
            T oldValue = (T) old;
            return oldValue;
        }
        T value = supplier.get();
        idemCache.put(key, value);
        return value;
    }

    public ScreenResult createOrGetScreenResult(Long candidateId, String jobCode, String idemKey) {
        return idempotent("screen:" + idemKey, () -> {
            String bizCode = "AISCREEN" + nowCode();
            ScreenResult result = new ScreenResult(candidateId, jobCode, bizCode);
            result.screenStatus = SCREEN_PROCESSING;
            screenStore.put(candidateId + ":" + jobCode, result);
            return result;
        });
    }

    public ScreenResult getScreenResult(Long candidateId, String jobCode) {
        ScreenResult result = screenStore.get(candidateId + ":" + jobCode);
        if (result == null) {
            throw new ApiException(ErrorCode.RESUME_SCREEN_FAILED, "AISCREEN_NONE", "筛选结果不存在");
        }
        return result;
    }

    public ScreenResult reviewScreenResult(Long candidateId, String jobCode, Integer reviewResult, String reviewComment, String idemKey) {
        return idempotent("screen-review:" + idemKey, () -> {
            ScreenResult result = getScreenResult(candidateId, jobCode);
            if (result.screenStatus != SCREEN_SUCCESS) {
                throw new ApiException(ErrorCode.AI_REVIEW_REQUIRED, result.bizCode, "筛选未成功，禁止复核");
            }
            result.reviewResult = reviewResult;
            result.reviewComment = reviewComment;
            result.reviewTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return result;
        });
    }

    public QuestionRecord createQuestionRecord(Long interviewId, String resumeSectionId, Integer difficulty, Integer questionCount, String idemKey) {
        return idempotent("question:" + idemKey, () -> {
            String requestCode = "INTA" + nowCode();
            QuestionRecord record = new QuestionRecord();
            record.requestCode = requestCode;
            record.interviewId = interviewId;
            record.resumeSectionId = resumeSectionId;
            record.difficultyLevel = difficulty;
            record.questionCount = questionCount;
            record.reviewStatus = REVIEW_PENDING;
            record.bizCode = requestCode;
            questionStore.put(requestCode, record);
            return record;
        });
    }

    public QuestionRecord reviewQuestion(String requestCode, Integer reviewStatus, String reviewComment, String idemKey) {
        return idempotent("question-review:" + idemKey, () -> {
            QuestionRecord record = questionStore.get(requestCode);
            if (record == null) {
                throw new ApiException(ErrorCode.PARAM_INVALID, requestCode, "题目记录不存在");
            }
            if (record.reviewStatus != REVIEW_PENDING) {
                throw new ApiException(ErrorCode.AI_REVIEW_REQUIRED, requestCode, "题目审核状态非法流转");
            }
            record.reviewStatus = reviewStatus;
            record.reviewComment = reviewComment;
            return record;
        });
    }

    public AnswerEvaluateResult evaluateAnswer(Long interviewId, Long questionId, String answerText, String idemKey) {
        return idempotent("answer-eval:" + idemKey, () -> {
            if (answerText == null || answerText.isBlank()) {
                throw new ApiException(ErrorCode.ANSWER_EVALUATE_FAILED, "ANSWER_EVAL", "回答内容为空");
            }
            AnswerEvaluateResult result = new AnswerEvaluateResult();
            result.bizCode = "ANS" + nowCode();
            result.interviewId = interviewId;
            result.questionId = questionId;
            result.accuracyScore = new BigDecimal("82.50");
            result.coverageScore = new BigDecimal("78.00");
            result.clarityScore = new BigDecimal("80.00");
            result.followUpSuggest = "请补充系统设计中的容量评估与降级方案。";
            return result;
        });
    }

    public ExportTask createExportTask(int exportType, String content, String jobCode, String idemKey) {
        return idempotent("export:" + idemKey, () -> {
            ExportTask task = new ExportTask();
            task.taskId = Math.abs(UUID.randomUUID().getMostSignificantBits());
            task.taskCode = "EXP" + nowCode();
            task.exportType = exportType;
            task.exportContent = content;
            task.jobCode = jobCode;
            task.taskStatus = TASK_PROCESSING;
            task.bizCode = task.taskCode;
            exportTaskStore.put(String.valueOf(task.taskId), task);
            return task;
        });
    }

    public ExportTask getExportTask(Long taskId) {
        ExportTask task = exportTaskStore.get(String.valueOf(taskId));
        if (task == null) {
            throw new ApiException(ErrorCode.EXPORT_TASK_NOT_FOUND, "EXP_NONE", "导出任务不存在");
        }
        return task;
    }

    public void markScreenSuccess(Long candidateId, String jobCode, double matchScore, int recommendLevel) {
        ScreenResult result = getScreenResult(candidateId, jobCode);
        result.screenStatus = SCREEN_SUCCESS;
        result.matchScore = BigDecimal.valueOf(matchScore);
        result.recommendLevel = recommendLevel;
    }

    private String nowCode() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public static class ScreenResult {
        public Long candidateId;
        public String jobCode;
        public Integer screenStatus;
        public BigDecimal matchScore;
        public Integer recommendLevel;
        public Integer reviewResult;
        public String reviewComment;
        public String reviewTime;
        public String bizCode;

        public ScreenResult(Long candidateId, String jobCode, String bizCode) {
            this.candidateId = candidateId;
            this.jobCode = jobCode;
            this.bizCode = bizCode;
        }
    }

    public static class QuestionRecord {
        public String requestCode;
        public Long interviewId;
        public String resumeSectionId;
        public Integer difficultyLevel;
        public Integer questionCount;
        public Integer reviewStatus;
        public String reviewComment;
        public String bizCode;
    }

    public static class AnswerEvaluateResult {
        public String bizCode;
        public Long interviewId;
        public Long questionId;
        public BigDecimal accuracyScore;
        public BigDecimal coverageScore;
        public BigDecimal clarityScore;
        public String followUpSuggest;
    }

    public static class ExportTask {
        public Long taskId;
        public String taskCode;
        public Integer exportType;
        public String exportContent;
        public String jobCode;
        public Integer taskStatus;
        public String bizCode;
    }
}
