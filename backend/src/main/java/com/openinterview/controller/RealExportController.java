package com.openinterview.controller;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.common.Result;
import com.openinterview.entity.ExportTaskEntity;
import com.openinterview.service.InMemoryWorkflowService;
import com.openinterview.service.RealExportService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v2/exports")
public class RealExportController {

    private final RealExportService realExportService;

    public RealExportController(RealExportService realExportService) {
        this.realExportService = realExportService;
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody @Valid RealExportCreateRequest request) {
        if (request.exportType == 0 && (request.jobCode == null || request.jobCode.isBlank())) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "岗位编码不能为空");
        }
        ExportTaskEntity task = realExportService.createAndRun(
                request.exportType,
                request.exportContent,
                request.jobCode,
                request.exportUserId,
                request.exportUserName);
        return Result.success(toPayload(task), TraceContext.getTraceId(), task.taskCode);
    }

    @GetMapping("/{taskId}")
    public Result<Map<String, Object>> get(@PathVariable("taskId") Long taskId) {
        ExportTaskEntity task = realExportService.getTask(taskId);
        return Result.success(toPayload(task), TraceContext.getTraceId(), task.taskCode);
    }

    @GetMapping("/{taskId}/download")
    public ResponseEntity<byte[]> download(@PathVariable("taskId") Long taskId) {
        ExportTaskEntity task = realExportService.getTask(taskId);
        byte[] body = realExportService.readCompletedFile(taskId);
        String ct = realExportService.contentTypeFor(task);
        String encoded = URLEncoder.encode(task.fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, ct)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(body);
    }

    private Map<String, Object> toPayload(ExportTaskEntity task) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.id);
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
        return data;
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

    public static class RealExportCreateRequest {
        @NotNull
        @Min(0)
        @Max(2)
        public Integer exportType;
        @NotBlank
        public String exportContent;
        public String jobCode;
        @NotNull
        public Long exportUserId;
        @NotBlank
        public String exportUserName;
    }
}
