package com.openinterview.service;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.entity.ExportTaskEntity;
import com.openinterview.export.ExportContext;
import com.openinterview.export.ExportHashUtil;
import com.openinterview.export.ExportResult;
import com.openinterview.export.ExportStrategy;
import com.openinterview.export.ExportStrategyFactory;
import com.openinterview.service.db.ExportTaskDbService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class RealExportService {

    @Value("${export.storage-dir:exports}")
    private String storageDir;

    private final ExportStrategyFactory exportStrategyFactory;
    private final ExportTaskDbService exportTaskDbService;

    public RealExportService(ExportStrategyFactory exportStrategyFactory,
                             ExportTaskDbService exportTaskDbService) {
        this.exportStrategyFactory = exportStrategyFactory;
        this.exportTaskDbService = exportTaskDbService;
    }

    public ExportTaskEntity createAndRun(int exportType, String exportContent, String jobCode,
                                         Long exportUserId, String exportUserName) {
        validate(exportType, exportContent, jobCode);

        ExportTaskEntity entity = new ExportTaskEntity();
        entity.taskCode = "V2EXP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        entity.exportType = exportType;
        entity.exportContent = exportContent.trim();
        entity.jobCode = jobCode;
        entity.exportUserId = exportUserId;
        entity.exportUserName = exportUserName;
        entity.taskStatus = InMemoryWorkflowService.TASK_PROCESSING;
        entity.isDeleted = 0;
        LocalDateTime now = LocalDateTime.now();
        entity.createTime = now;
        entity.updateTime = now;
        exportTaskDbService.create(entity);

        ExportContext ctx = new ExportContext();
        ctx.exportType = exportType;
        ctx.exportContent = entity.exportContent;
        ctx.jobCode = jobCode;
        ctx.taskCode = entity.taskCode;

        try {
            ExportStrategy strategy = exportStrategyFactory.getStrategy(exportType);
            ExportResult result = strategy.export(ctx);
            saveToDisk(entity.id, result.fileName, result.fileBytes);
            entity.fileName = result.fileName;
            entity.fileUrl = entity.id + "/" + result.fileName;
            entity.fileSize = (long) result.fileBytes.length;
            entity.fileHash = ExportHashUtil.sha256Hex(result.fileBytes);
            entity.taskStatus = InMemoryWorkflowService.TASK_SUCCESS;
            entity.updateTime = LocalDateTime.now();
            exportTaskDbService.updateById(entity);
            return exportTaskDbService.getById(entity.id);
        } catch (ApiException ex) {
            markFailed(entity, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            markFailed(entity, ex.getMessage());
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_FAIL", "导出失败: " + ex.getMessage());
        }
    }

    private void markFailed(ExportTaskEntity entity, String reason) {
        entity.taskStatus = InMemoryWorkflowService.TASK_FAILED;
        entity.failReason = reason;
        entity.updateTime = LocalDateTime.now();
        exportTaskDbService.updateById(entity);
    }

    private void validate(int exportType, String exportContent, String jobCode) {
        if (exportType < 0 || exportType > 2) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_TYPE", "exportType 仅支持 0/1/2");
        }
        if (exportContent == null || exportContent.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "exportContent 不能为空");
        }
        if (exportType == 0 && (jobCode == null || jobCode.isBlank())) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "岗位编码不能为空");
        }
    }

    private Path storageRoot() {
        return Paths.get(storageDir).toAbsolutePath().normalize();
    }

    private Path saveToDisk(Long taskId, String fileName, byte[] bytes) throws IOException {
        Path root = storageRoot();
        Path dir = root.resolve(String.valueOf(taskId));
        Files.createDirectories(dir);
        String safeName = fileName.replace("..", "_");
        Path file = dir.resolve(safeName);
        Files.write(file, bytes);
        return file;
    }

    public ExportTaskEntity getTask(Long taskId) {
        ExportTaskEntity entity = exportTaskDbService.getById(taskId);
        if (entity == null) {
            throw new ApiException(ErrorCode.EXPORT_TASK_NOT_FOUND, "EXP_NONE", "导出任务不存在");
        }
        return entity;
    }

    public byte[] readCompletedFile(Long taskId) {
        ExportTaskEntity entity = getTask(taskId);
        if (entity.taskStatus == null
                || entity.taskStatus != InMemoryWorkflowService.TASK_SUCCESS) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_STATE", "任务未完成或失败，无法下载");
        }
        if (entity.fileName == null || entity.fileName.isBlank()) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_FILE", "文件信息缺失");
        }
        Path file = storageRoot().resolve(String.valueOf(taskId)).resolve(entity.fileName.replace("..", "_"));
        try {
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_READ", "读取导出文件失败");
        }
    }

    public String contentTypeFor(ExportTaskEntity entity) {
        if (entity.exportType == null) {
            return "application/octet-stream";
        }
        return switch (entity.exportType) {
            case 0, 1 -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case 2 -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
