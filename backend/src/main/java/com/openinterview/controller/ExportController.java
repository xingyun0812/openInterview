package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.service.EventMappingService;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private final InMemoryWorkflowService workflowService;
    private final EventMappingService eventMappingService;
    private final com.openinterview.service.EventBridgeService eventBridgeService;

    public ExportController(InMemoryWorkflowService workflowService,
                            EventMappingService eventMappingService,
                            com.openinterview.service.EventBridgeService eventBridgeService) {
        this.workflowService = workflowService;
        this.eventMappingService = eventMappingService;
        this.eventBridgeService = eventBridgeService;
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
        data.put("exportType", task.exportType);
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
        data.put("exportType", task.exportType);
        data.put("mqEventCode", mqEvent);
        data.put("webhookEventCode", webhookEvent);
        eventBridgeService.publish(mqEvent, task.bizCode, data);
        return Result.success(data, TraceContext.getTraceId(), task.bizCode);
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
