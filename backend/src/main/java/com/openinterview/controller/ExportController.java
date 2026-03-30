package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.EvidenceStore;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.trace.TraceContext;
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

    public ExportController(InMemoryWorkflowService workflowService,
                            EventMappingService eventMappingService,
                            com.openinterview.service.EventBridgeService eventBridgeService,
                            EvidenceStore evidenceStore) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
        this.evidenceStore = evidenceStore;
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
        evidenceStore.addExportAudit(auditPayload("EXPORT_RETRY", task));
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
        data.put("failReason", task.failReason);
        data.put("retryCount", task.retryCount);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, task.bizCode, data);
        evidenceStore.addExportAudit(auditPayload("EXPORT_CREATE", task));
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
}
