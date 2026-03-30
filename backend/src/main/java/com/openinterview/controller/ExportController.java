package com.openinterview.controller;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.common.Result;
import com.openinterview.service.AuditTrailService;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.EvidenceStore;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/api/v1/export")
public class ExportController {
    private static final Logger log = LoggerFactory.getLogger(ExportController.class);
    private final InMemoryWorkflowService workflowService;
    private final EventMappingService eventMappingService;
    private final com.openinterview.service.EventBridgeService eventBridgeService;
    private final EvidenceStore evidenceStore;
    private final AuditTrailService auditTrailService;

    public ExportController(InMemoryWorkflowService workflowService,
                            EventMappingService eventMappingService,
                            com.openinterview.service.EventBridgeService eventBridgeService,
                            EvidenceStore evidenceStore,
                            AuditTrailService auditTrailService) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
        this.evidenceStore = evidenceStore;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/task")
    public Result<Map<String, Object>> unifiedExportTask(@RequestBody @Validated UnifiedExportTaskRequest request,
                                                         @RequestHeader("X-Idempotency-Key") String idemKey) {
        int et = request.exportType;
        if (et == InMemoryWorkflowService.EXPORT_SCREENING_EXCEL) {
            if (request.candidateIds == null || request.candidateIds.isEmpty()) {
                throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "candidateIds 不能为空");
            }
            if (request.jobCode == null || request.jobCode.isBlank()) {
                throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "jobCode 不能为空");
            }
            String content = request.candidateIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            return createExport(et, content, request.jobCode, idemKey);
        }
        if (et == InMemoryWorkflowService.EXPORT_INTERVIEW_EXCEL
                || et == InMemoryWorkflowService.EXPORT_INTERVIEW_WORD) {
            if (request.interviewIds == null || request.interviewIds.isEmpty()) {
                throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "interviewIds 不能为空");
            }
            String content = request.interviewIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            return createExport(et, content, null, idemKey);
        }
        throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_TYPE", "exportType 非法");
    }

    @PostMapping("/screening/excel")
    public Result<Map<String, Object>> screeningExcel(@RequestBody @Validated ScreeningExportRequest request,
                                                      @RequestHeader("X-Idempotency-Key") String idemKey) {
        return createExport(InMemoryWorkflowService.EXPORT_SCREENING_EXCEL,
                request.candidateIds.stream().map(String::valueOf).collect(Collectors.joining(",")),
                request.jobCode, idemKey);
    }

    @PostMapping("/excel")
    public Result<Map<String, Object>> interviewExcel(@RequestBody @Validated InterviewExportRequest request,
                                                      @RequestHeader("X-Idempotency-Key") String idemKey) {
        return createExport(InMemoryWorkflowService.EXPORT_INTERVIEW_EXCEL,
                request.interviewIds.stream().map(String::valueOf).collect(Collectors.joining(",")),
                null, idemKey);
    }

    @PostMapping("/word")
    public Result<Map<String, Object>> interviewWord(@RequestBody @Validated InterviewExportRequest request,
                                                     @RequestHeader("X-Idempotency-Key") String idemKey) {
        return createExport(InMemoryWorkflowService.EXPORT_INTERVIEW_WORD,
                request.interviewIds.stream().map(String::valueOf).collect(Collectors.joining(",")),
                null, idemKey);
    }

    @GetMapping("/task/{taskId}")
    public Result<Map<String, Object>> taskStatus(@PathVariable("taskId") Long taskId) {
        InMemoryWorkflowService.ExportTask task = workflowService.getExportTask(taskId);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.taskId);
        data.put("taskCode", task.taskCode);
        data.put("taskStatus", task.taskStatus);
        data.put("taskStatusLabel", taskStatusLabel(task.taskStatus));
        data.put("exportType", task.exportType);
        data.put("exportTypeLabel", exportTypeLabel(task.exportType));
        data.put("fileUrl", task.fileUrl);
        data.put("fileName", task.fileName);
        data.put("fileSize", task.fileSize);
        data.put("fileHash", task.fileHash);
        data.put("failReason", task.failReason);
        data.put("retryCount", task.retryCount);
        data.put("stateFlow", task.stateFlow);
        log.info("traceId={} bizCode={} action=export.task.query taskId={} taskStatus={}",
                TraceContext.getTraceId(), task.bizCode, task.taskId, task.taskStatus);
        return Result.success(data, TraceContext.getTraceId(), task.bizCode);
    }

    @PostMapping("/task/{taskId}/retry")
    public Result<Map<String, Object>> retryTask(@PathVariable("taskId") Long taskId,
                                                 @RequestHeader("X-Idempotency-Key") String idemKey) {
        InMemoryWorkflowService.ExportTask task = workflowService.retryExportTask(taskId, idemKey);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.taskId);
        data.put("taskCode", task.taskCode);
        data.put("taskStatus", task.taskStatus);
        data.put("taskStatusLabel", taskStatusLabel(task.taskStatus));
        data.put("retryCount", task.retryCount);
        data.put("failReason", task.failReason);
        data.put("fileHash", task.fileHash);
        data.put("fileSize", task.fileSize);
        evidenceStore.addExportAudit(auditPayload("EXPORT_RETRY", task));
        auditTrailService.record("export", "export.retry", task.bizCode,
                task.taskStatus == InMemoryWorkflowService.TASK_FAILED ? task.lastErrorCode : "0",
                "taskId=" + task.taskId + " retryCount=" + task.retryCount + " taskStatus=" + task.taskStatus);
        log.info("traceId={} bizCode={} action=export.task.retry taskId={} taskStatus={} retryCount={}",
                TraceContext.getTraceId(), task.bizCode, task.taskId, task.taskStatus, task.retryCount);
        return Result.success(data, TraceContext.getTraceId(), task.bizCode);
    }

    private Result<Map<String, Object>> createExport(int exportType, String content, String jobCode, String idemKey) {
        InMemoryWorkflowService.ExportTask task = workflowService.createExportTask(exportType, content, jobCode, idemKey);
        String mqEvent = "export.generate";
        String webhookEvent = eventMappingService.toWebhookEvent(mqEvent);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.taskId);
        data.put("taskCode", task.taskCode);
        data.put("taskStatus", task.taskStatus);
        data.put("taskStatusLabel", taskStatusLabel(task.taskStatus));
        data.put("exportType", task.exportType);
        data.put("exportTypeLabel", exportTypeLabel(task.exportType));
        data.put("fileHash", task.fileHash);
        data.put("fileName", task.fileName);
        data.put("fileUrl", task.fileUrl);
        data.put("fileSize", task.fileSize);
        data.put("failReason", task.failReason);
        data.put("retryCount", task.retryCount);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, task.bizCode, data);
        evidenceStore.addExportAudit(auditPayload("EXPORT_CREATE", task));
        auditTrailService.record("export", "export.create", task.bizCode,
                task.taskStatus == InMemoryWorkflowService.TASK_FAILED ? task.lastErrorCode : "0",
                "exportType=" + task.exportType + " taskId=" + task.taskId + " taskStatus=" + task.taskStatus);
        if (task.taskStatus == InMemoryWorkflowService.TASK_FAILED) {
            log.error("traceId={} bizCode={} errorCode=6003 action=export.task.create.failed taskId={} reason={}",
                    TraceContext.getTraceId(), task.bizCode, task.taskId, task.failReason);
        } else {
            log.info("traceId={} bizCode={} action=export.task.create taskId={} exportType={} taskStatus={}",
                    TraceContext.getTraceId(), task.bizCode, task.taskId, task.exportType, task.taskStatus);
        }
        return Result.success(data, TraceContext.getTraceId(), task.bizCode);
    }

    private Map<String, Object> auditPayload(String action, InMemoryWorkflowService.ExportTask task) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("traceId", TraceContext.getTraceId());
        payload.put("bizCode", task.bizCode);
        payload.put("taskId", task.taskId);
        payload.put("taskStatus", task.taskStatus);
        payload.put("taskStatusLabel", taskStatusLabel(task.taskStatus));
        payload.put("exportType", task.exportType);
        payload.put("exportTypeLabel", exportTypeLabel(task.exportType));
        payload.put("errorCode", task.lastErrorCode);
        payload.put("failReason", task.failReason);
        payload.put("fileHash", task.fileHash);
        payload.put("time", java.time.LocalDateTime.now().toString());
        return payload;
    }

    private String exportTypeLabel(Integer exportType) {
        if (exportType == null) {
            return "未知";
        }
        if (exportType == InMemoryWorkflowService.EXPORT_SCREENING_EXCEL) {
            return "筛选Excel";
        }
        if (exportType == InMemoryWorkflowService.EXPORT_INTERVIEW_EXCEL) {
            return "成绩Excel";
        }
        if (exportType == InMemoryWorkflowService.EXPORT_INTERVIEW_WORD) {
            return "面试Word";
        }
        return "未知";
    }

    private String taskStatusLabel(Integer taskStatus) {
        if (taskStatus == null) {
            return "未知";
        }
        if (taskStatus == InMemoryWorkflowService.TASK_PROCESSING) {
            return "处理中";
        }
        if (taskStatus == InMemoryWorkflowService.TASK_SUCCESS) {
            return "已完成";
        }
        if (taskStatus == InMemoryWorkflowService.TASK_FAILED) {
            return "失败";
        }
        return "未知";
    }

    public static class ScreeningExportRequest {
        @NotNull
        @Size(min = 1)
        public List<Long> candidateIds;
        @NotBlank
        public String jobCode;
    }

    public static class InterviewExportRequest {
        @NotNull
        @Size(min = 1)
        public List<Long> interviewIds;
    }

    public static class UnifiedExportTaskRequest {
        @NotNull
        @Min(0)
        @Max(2)
        public Integer exportType;
        public List<Long> candidateIds;
        public String jobCode;
        public List<Long> interviewIds;
    }
}
