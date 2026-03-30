package com.openinterview.service;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class InMemoryWorkflowService {
    public static final int PARSE_PROCESSING = 1;
    public static final int PARSE_SUCCESS = 2;
    public static final int PARSE_FAILED = 3;

    public static final int SCREEN_PROCESSING = 1;
    public static final int SCREEN_SUCCESS = 2;
    public static final int SCREEN_FAILED = 3;
    public static final int SCREEN_CANCELED = 4;

    public static final int REVIEW_PENDING = 1;
    public static final int REVIEW_APPROVED = 2;
    public static final int REVIEW_REJECTED = 3;
    public static final int SCREEN_REVIEW_PASS = 1;
    public static final int SCREEN_REVIEW_PENDING = 2;
    public static final int SCREEN_REVIEW_REJECT = 3;

    public static final int EXPORT_SCREENING_EXCEL = 0;
    public static final int EXPORT_INTERVIEW_EXCEL = 1;
    public static final int EXPORT_INTERVIEW_WORD = 2;

    public static final int TASK_PROCESSING = 1;
    public static final int TASK_SUCCESS = 2;
    public static final int TASK_FAILED = 3;
    private static final int EXPORT_MAX_RETRY = 3;
    private static final int PARSE_MAX_ATTEMPTS = 3;

    private final Map<String, Object> idemCache = new ConcurrentHashMap<>();
    private final Map<String, StoredResume> resumeStore = new ConcurrentHashMap<>();
    private final Map<Long, ResumeParseResult> parseResultStore = new ConcurrentHashMap<>();
    private final Map<String, ParseTask> parseTaskStore = new ConcurrentHashMap<>();
    private final java.util.List<ParseFailureAudit> parseFailureAudits = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<String, ScreenResult> screenStore = new ConcurrentHashMap<>();
    private final Map<String, QuestionRecord> questionStore = new ConcurrentHashMap<>();
    private final Map<String, ExportTask> exportTaskStore = new ConcurrentHashMap<>();
    /** 对应 interview_answer_assess_record，后续可换 DB */
    private final Map<String, AnswerAssessRecord> answerAssessStore = new ConcurrentHashMap<>();

    public <T> T idempotent(String key, Supplier<T> supplier) {
        @SuppressWarnings("unchecked")
        T value = (T) idemCache.computeIfAbsent(key, k -> supplier.get());
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

    public UploadResult uploadResume(Long candidateId, String originalFilename, byte[] content, String idemKey) {
        return idempotent("resume-upload:" + idemKey, () -> {
            String bizCode = "RESUMEUPLOAD" + nowCode();
            String safeName = (originalFilename == null || originalFilename.isBlank()) ? "resume.pdf" : originalFilename;
            String resumeUrl = "mock://resume/" + candidateId + "/" + UUID.randomUUID() + "-" + safeName;
            StoredResume storedResume = new StoredResume();
            storedResume.candidateId = candidateId;
            storedResume.resumeUrl = resumeUrl;
            storedResume.originalFilename = safeName;
            storedResume.size = content.length;
            storedResume.content = content;
            resumeStore.put(resumeUrl, storedResume);

            UploadResult result = new UploadResult();
            result.candidateId = candidateId;
            result.resumeUrl = resumeUrl;
            result.bizCode = bizCode;
            return result;
        });
    }

    public ParseTask createOrGetParseTask(Long candidateId, String resumeUrl, String idemKey, String traceId) {
        return idempotent("resume-parse:" + idemKey, () -> {
            if (!resumeStore.containsKey(resumeUrl)) {
                throw new ApiException(ErrorCode.RESUME_PARSE_FAILED, "AIPARSE_INVALID", "简历地址不存在或未上传");
            }
            String taskCode = "AIPARSE" + nowCode();
            ParseTask task = new ParseTask();
            task.taskCode = taskCode;
            task.candidateId = candidateId;
            task.resumeUrl = resumeUrl;
            task.parseStatus = PARSE_PROCESSING;
            task.retryCount = 0;
            task.maxAttempts = PARSE_MAX_ATTEMPTS;
            task.traceId = traceId;
            task.bizCode = taskCode;
            parseTaskStore.put(taskCode, task);

            ResumeParseResult result = new ResumeParseResult();
            result.candidateId = candidateId;
            result.resumeUrl = resumeUrl;
            result.parseStatus = PARSE_PROCESSING;
            result.bizCode = taskCode;
            parseResultStore.put(candidateId, result);
            return task;
        });
    }

    public ResumeParseResult getParseResult(Long candidateId) {
        ResumeParseResult result = parseResultStore.get(candidateId);
        if (result == null) {
            throw new ApiException(ErrorCode.RESUME_PARSE_FAILED, "AIPARSE_NONE", "解析结果不存在");
        }
        return result;
    }

    public synchronized ParseAttemptResult executeParseAttempt(String taskCode) {
        ParseTask task = parseTaskStore.get(taskCode);
        if (task == null) {
            throw new ApiException(ErrorCode.RESUME_PARSE_FAILED, "AIPARSE_NONE", "解析任务不存在");
        }
        task.retryCount += 1;
        int attempt = task.retryCount;
        ResumeParseResult result = getParseResult(task.candidateId);
        result.parseStatus = PARSE_PROCESSING;

        ParseAttemptResult attemptResult = new ParseAttemptResult();
        attemptResult.taskCode = task.taskCode;
        attemptResult.bizCode = task.bizCode;
        attemptResult.candidateId = task.candidateId;
        attemptResult.traceId = task.traceId;
        attemptResult.currentAttempt = attempt;

        if (task.resumeUrl.contains("fail")) {
            result.parseStatus = PARSE_FAILED;
            result.failReason = "mock parse failed at attempt " + attempt;
            result.errorCode = String.valueOf(ErrorCode.RESUME_PARSE_FAILED.getCode());
            result.basicInfo = null;
            result.education = java.util.List.of();
            result.workExperience = java.util.List.of();
            result.skillTags = java.util.List.of();
            attemptResult.success = false;
            attemptResult.exhausted = attempt >= task.maxAttempts;
            attemptResult.errorCode = result.errorCode;
            attemptResult.failReason = result.failReason;
            if (!attemptResult.exhausted) {
                result.parseStatus = PARSE_PROCESSING;
            }
            return attemptResult;
        }

        result.parseStatus = PARSE_SUCCESS;
        result.basicInfo = java.util.Map.of("candidateId", task.candidateId, "name", "候选人" + task.candidateId);
        result.education = java.util.List.of(java.util.Map.of("school", "示例大学", "degree", "本科"));
        result.workExperience = java.util.List.of(java.util.Map.of("company", "示例科技", "title", "后端工程师"));
        result.skillTags = java.util.List.of("Java", "Spring Boot", "MySQL");
        result.failReason = null;
        result.errorCode = null;
        attemptResult.success = true;
        attemptResult.exhausted = false;
        return attemptResult;
    }

    public void addParseFailureAudit(String traceId, String bizCode, String errorCode, String failReason, int retryCount, boolean exhausted) {
        ParseFailureAudit audit = new ParseFailureAudit();
        audit.traceId = traceId;
        audit.bizCode = bizCode;
        audit.errorCode = errorCode;
        audit.failReason = failReason;
        audit.retryCount = retryCount;
        audit.exhausted = exhausted;
        audit.occurTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        parseFailureAudits.add(audit);
    }

    public java.util.List<ParseFailureAudit> getParseFailureAudits() {
        return java.util.List.copyOf(parseFailureAudits);
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
            if (result.matchScore == null || result.recommendLevel == null) {
                throw new ApiException(ErrorCode.AI_REVIEW_REQUIRED, result.bizCode, "筛选结果不完整，禁止复核");
            }
            if (!isValidScreenReviewResult(reviewResult)) {
                throw new ApiException(ErrorCode.PARAM_INVALID, result.bizCode, "复核结论非法，仅允许 1/2/3");
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

            AnswerAssessRecord stored = new AnswerAssessRecord();
            stored.recordCode = result.bizCode;
            stored.interviewId = interviewId;
            stored.questionId = questionId;
            stored.answerText = answerText;
            stored.accuracyScore = result.accuracyScore;
            stored.coverageScore = result.coverageScore;
            stored.clarityScore = result.clarityScore;
            stored.followUpSuggest = result.followUpSuggest;
            stored.createTime = nowReadable();
            answerAssessStore.put(result.bizCode, stored);
            return result;
        });
    }

    public java.util.List<AnswerAssessRecord> getAnswerAssessRecords() {
        return java.util.List.copyOf(answerAssessStore.values());
    }

    public ExportTask createExportTask(int exportType, String content, String jobCode, String idemKey) {
        return idempotent("export:" + idemKey, () -> {
            ExportTask task = new ExportTask();
            task.taskId = Math.abs(UUID.randomUUID().getMostSignificantBits());
            task.taskCode = "EXP" + nowCode();
            task.exportType = exportType;
            task.exportContent = content;
            task.jobCode = jobCode;
            task.taskStatus = TASK_SUCCESS;
            task.fileName = "export_" + task.taskCode + ".dat";
            task.fileUrl = "/mock/" + task.fileName;
            task.fileSize = 1024L;
            task.fileHash = UUID.randomUUID().toString().replace("-", "");
            task.failReason = null;
            task.retryCount = 0;
            task.stateFlow.clear();
            task.stateFlow.add("1->2@" + nowReadable());
            task.lastErrorCode = null;
            task.bizCode = task.taskCode;
            task.retryCount = 0;
            exportTaskStore.put(String.valueOf(task.taskId), task);
            processExportTask(task);
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

    public ExportTask retryExportTask(Long taskId, String idemKey) {
        return idempotent("export-retry:" + idemKey, () -> {
            ExportTask task = getExportTask(taskId);
            int nextRetry = task.retryCount + 1;
            if (nextRetry >= EXPORT_MAX_RETRY) {
                task.retryCount = nextRetry;
                task.taskStatus = TASK_FAILED;
                task.failReason = "重试次数已耗尽";
                task.lastErrorCode = String.valueOf(ErrorCode.EXPORT_FILE_FAILED.getCode());
                task.stateFlow.add("3->3@" + nowReadable());
                return task;
            }
            if (task.taskStatus != TASK_FAILED) {
                throw new ApiException(ErrorCode.PARAM_INVALID, task.bizCode, "仅失败任务允许重试");
            }
            task.retryCount = nextRetry;
            task.taskStatus = TASK_PROCESSING;
            task.failReason = null;
            task.lastErrorCode = null;
            task.stateFlow.add("3->1@" + nowReadable());
            processExportTask(task);
            return task;
        });
    }

    public void markScreenSuccess(Long candidateId, String jobCode, double matchScore, int recommendLevel) {
        ScreenResult result = getScreenResult(candidateId, jobCode);
        transitScreenStatus(result, SCREEN_SUCCESS);
        result.matchScore = BigDecimal.valueOf(matchScore);
        result.recommendLevel = recommendLevel;
        result.reasonSummary = "AI仅输出建议，需HR复核后生效";
    }

    public void markScreenFailed(Long candidateId, String jobCode, String failReason) {
        ScreenResult result = getScreenResult(candidateId, jobCode);
        transitScreenStatus(result, SCREEN_FAILED);
        result.failReason = failReason;
    }

    public void retryScreen(Long candidateId, String jobCode) {
        ScreenResult result = getScreenResult(candidateId, jobCode);
        transitScreenStatus(result, SCREEN_PROCESSING);
    }

    private void transitScreenStatus(ScreenResult result, int targetStatus) {
        int current = result.screenStatus;
        boolean allowed = (current == SCREEN_PROCESSING && (targetStatus == SCREEN_SUCCESS || targetStatus == SCREEN_FAILED || targetStatus == SCREEN_CANCELED))
                || (current == SCREEN_FAILED && targetStatus == SCREEN_PROCESSING);
        if (!allowed) {
            throw new ApiException(ErrorCode.SCREEN_STATUS_ILLEGAL, result.bizCode, "screen_status 非法流转: " + current + "->" + targetStatus);
        }
        result.screenStatus = targetStatus;
    }

    private boolean isValidScreenReviewResult(Integer reviewResult) {
        return reviewResult != null
                && (reviewResult == SCREEN_REVIEW_PASS
                || reviewResult == SCREEN_REVIEW_PENDING
                || reviewResult == SCREEN_REVIEW_REJECT);
    }

    private void processExportTask(ExportTask task) {
        try {
            if (task.exportType != EXPORT_SCREENING_EXCEL) {
                task.fileName = task.taskCode + ".xlsx";
                task.fileUrl = "/downloads/" + task.fileName;
                task.fileSize = 0L;
                task.fileHash = sha256(task.taskCode + "|" + task.exportType);
                task.taskStatus = TASK_SUCCESS;
                task.stateFlow.add("1->2@" + nowReadable());
                return;
            }
            List<Map<String, Object>> rows = buildScreeningRows(task.exportContent, task.jobCode);
            String fileContent = toCsv(rows);
            task.screeningRows = rows;
            task.fileName = "screening-" + task.taskCode + ".csv";
            task.fileUrl = "/downloads/" + task.fileName;
            task.fileSize = (long) fileContent.getBytes(StandardCharsets.UTF_8).length;
            task.fileHash = sha256(fileContent);
            task.taskStatus = TASK_SUCCESS;
            task.stateFlow.add("1->2@" + nowReadable());
        } catch (Exception ex) {
            task.taskStatus = TASK_FAILED;
            task.failReason = ex.getMessage();
            task.lastErrorCode = String.valueOf(ErrorCode.EXPORT_FILE_FAILED.getCode());
            task.stateFlow.add("1->3@" + nowReadable());
        }
    }

    private List<Map<String, Object>> buildScreeningRows(String content, String jobCode) {
        if (jobCode == null || jobCode.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "岗位编码不能为空");
        }
        if (jobCode.startsWith("FAIL")) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_FAIL", "导出模拟失败");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] parts = content.split(",");
        for (String part : parts) {
            Long candidateId = Long.parseLong(part.trim());
            if (candidateId <= 0) {
                throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "候选人ID非法");
            }
            Map<String, Object> row = new ConcurrentHashMap<>();
            row.put("candidateName", "候选人" + candidateId);
            row.put("applyPosition", jobCode);
            row.put("matchScore", BigDecimal.valueOf(80 + (candidateId % 20)).setScale(2));
            row.put("recommendLevel", candidateId % 3 == 0 ? "待定" : "推荐");
            row.put("reviewResult", candidateId % 2 == 0 ? "通过筛选" : "待定");
            row.put("reviewUser", "HR-" + (candidateId % 10));
            row.put("reviewTime", nowReadable());
            rows.add(row);
        }
        return rows;
    }

    private String toCsv(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("candidateName,applyPosition,matchScore,recommendLevel,reviewResult,reviewUser,reviewTime\n");
        for (Map<String, Object> row : rows) {
            sb.append(row.get("candidateName")).append(',')
                    .append(row.get("applyPosition")).append(',')
                    .append(row.get("matchScore")).append(',')
                    .append(row.get("recommendLevel")).append(',')
                    .append(row.get("reviewResult")).append(',')
                    .append(row.get("reviewUser")).append(',')
                    .append(row.get("reviewTime")).append('\n');
        }
        return sb.toString();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_HASH", "计算文件哈希失败");
        }
    }

    private String nowCode() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String nowReadable() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static class ScreenResult {
        public Long candidateId;
        public String jobCode;
        public Integer screenStatus;
        public BigDecimal matchScore;
        public Integer recommendLevel;
        public String reasonSummary;
        public Integer reviewResult;
        public String reviewComment;
        public String reviewTime;
        public String failReason;
        public String bizCode;

        public ScreenResult(Long candidateId, String jobCode, String bizCode) {
            this.candidateId = candidateId;
            this.jobCode = jobCode;
            this.bizCode = bizCode;
        }
    }

    public static class UploadResult {
        public Long candidateId;
        public String resumeUrl;
        public String bizCode;
    }

    public static class StoredResume {
        public Long candidateId;
        public String resumeUrl;
        public String originalFilename;
        public Integer size;
        public byte[] content;
    }

    public static class ParseTask {
        public String taskCode;
        public Long candidateId;
        public String resumeUrl;
        public Integer parseStatus;
        public Integer retryCount;
        public Integer maxAttempts;
        public String traceId;
        public String bizCode;
    }

    public static class ParseAttemptResult {
        public String taskCode;
        public String bizCode;
        public Long candidateId;
        public String traceId;
        public Integer currentAttempt;
        public boolean success;
        public boolean exhausted;
        public String errorCode;
        public String failReason;
    }

    public static class ResumeParseResult {
        public Long candidateId;
        public String resumeUrl;
        public Integer parseStatus;
        public Map<String, Object> basicInfo;
        public java.util.List<Map<String, Object>> education;
        public java.util.List<Map<String, Object>> workExperience;
        public java.util.List<String> skillTags;
        public String failReason;
        public String errorCode;
        public String bizCode;
    }

    public static class ParseFailureAudit {
        public String traceId;
        public String bizCode;
        public String errorCode;
        public String failReason;
        public Integer retryCount;
        public Boolean exhausted;
        public String occurTime;
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

    /** 与 interview_answer_assess_record 对齐的内存持久化模型 */
    public static class AnswerAssessRecord {
        public String recordCode;
        public Long interviewId;
        public Long questionId;
        public String answerText;
        public BigDecimal accuracyScore;
        public BigDecimal coverageScore;
        public BigDecimal clarityScore;
        public String followUpSuggest;
        public String createTime;
    }

    public static class ExportTask {
        public Long taskId;
        public String taskCode;
        public Integer exportType;
        public String exportContent;
        public String jobCode;
        public Integer taskStatus;
        public String fileUrl;
        public String fileName;
        public Long fileSize;
        public String fileHash;
        public String failReason;
        public Integer retryCount;
        public String lastErrorCode;
        public String bizCode;
        public List<String> stateFlow = new ArrayList<>();
        public List<Map<String, Object>> screeningRows = new ArrayList<>();
    }
}
