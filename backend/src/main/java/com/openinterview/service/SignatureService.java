package com.openinterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.entity.OperationLogEntity;
import com.openinterview.entity.SignatureEntity;
import com.openinterview.service.db.AuditLogDbService;
import com.openinterview.service.db.SignatureDbService;
import com.openinterview.signature.SignatureHashUtil;
import com.openinterview.signature.SignatureStatus;
import com.openinterview.trace.TraceContext;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SignatureService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SignatureDbService signatureDbService;
    private final AuditTrailService auditTrailService;
    private final AuditLogDbService auditLogDbService;

    public SignatureService(SignatureDbService signatureDbService,
                            AuditTrailService auditTrailService,
                            AuditLogDbService auditLogDbService) {
        this.signatureDbService = signatureDbService;
        this.auditTrailService = auditTrailService;
        this.auditLogDbService = auditLogDbService;
    }

    public SignatureEntity generate(Long interviewId, Long signUserId, String signUserName, Integer signType) {
        if (interviewId == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "interviewId 不能为空");
        }
        if (signUserId == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "signUserId 不能为空");
        }
        if (signUserName == null || signUserName.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "signUserName 不能为空");
        }
        if (signType == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "signType 不能为空");
        }

        SignatureEntity entity = new SignatureEntity();
        entity.signCode = "SIG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        entity.interviewId = interviewId;
        entity.signUserId = signUserId;
        entity.signUserName = signUserName.trim();
        entity.signType = signType;
        entity.status = SignatureStatus.PENDING;
        entity.isDeleted = 0;
        LocalDateTime now = LocalDateTime.now();
        entity.createTime = now;
        entity.updateTime = now;
        signatureDbService.create(entity);
        auditTrailService.record("signature", "generate", entity.signCode, "0", "创建待签记录");
        return signatureDbService.getById(entity.id);
    }

    public SignatureEntity sign(Long signatureId, String signImgUrl, String signIp, String deviceInfo) {
        SignatureEntity entity = mustGet(signatureId);
        if (entity.status == null || entity.status != SignatureStatus.PENDING) {
            throw new ApiException(ErrorCode.SIGNATURE_STATUS_ILLEGAL, "SIG_STATE", "当前状态不可签署");
        }
        if (signImgUrl == null || signImgUrl.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "signImgUrl 不能为空");
        }
        if (signIp == null || signIp.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "signIp 不能为空");
        }
        entity.signImgUrl = signImgUrl.trim();
        entity.signIp = signIp.trim();
        entity.signDevice = deviceInfo == null ? null : deviceInfo.trim();
        entity.signTime = LocalDateTime.now();
        entity.fileHash = SignatureHashUtil.sha256Hex(toSignPayload(entity));
        entity.status = SignatureStatus.SIGNED;
        entity.updateTime = LocalDateTime.now();
        signatureDbService.updateById(entity);
        auditTrailService.record("signature", "sign", entity.signCode, "0", "补齐签名信息并生成哈希");
        return signatureDbService.getById(entity.id);
    }

    public VerifyResult verify(Long signatureId) {
        SignatureEntity entity = mustGet(signatureId);
        if (entity.status == null || entity.status != SignatureStatus.SIGNED) {
            throw new ApiException(ErrorCode.SIGNATURE_STATUS_ILLEGAL, "SIG_STATE", "当前状态不可验签");
        }
        validateSignedFields(entity);
        String expected = SignatureHashUtil.sha256Hex(toSignPayload(entity));
        if (entity.fileHash == null || entity.fileHash.isBlank()) {
            throw new ApiException(ErrorCode.SIGNATURE_VERIFY_FAILED, "SIG_VERIFY", "fileHash 缺失");
        }
        if (!entity.fileHash.equals(expected)) {
            throw new ApiException(ErrorCode.SIGNATURE_VERIFY_FAILED, "SIG_VERIFY", "哈希不一致，验签失败");
        }
        entity.status = SignatureStatus.VERIFIED;
        entity.updateTime = LocalDateTime.now();
        signatureDbService.updateById(entity);
        auditTrailService.record("signature", "verify", entity.signCode, "0", "验签通过");
        VerifyResult res = new VerifyResult();
        res.verified = true;
        res.signature = signatureDbService.getById(entity.id);
        return res;
    }

    public ArchiveResult archive(Long signatureId) {
        SignatureEntity entity = mustGet(signatureId);
        if (entity.status == null || entity.status != SignatureStatus.VERIFIED) {
            throw new ApiException(ErrorCode.SIGNATURE_STATUS_ILLEGAL, "SIG_STATE", "当前状态不可归档");
        }
        try {
            Path root = Paths.get("signatures").toAbsolutePath().normalize();
            Files.createDirectories(root);
            String safe = (entity.signCode == null ? ("sig-" + entity.id) : entity.signCode).replace("..", "_");
            Path file = root.resolve(safe + ".json");
            Files.writeString(file, MAPPER.writeValueAsString(toArchiveDoc(entity)));
            entity.status = SignatureStatus.ARCHIVED;
            entity.updateTime = LocalDateTime.now();
            signatureDbService.updateById(entity);
            auditTrailService.record("signature", "archive", entity.signCode, "0", "签名归档至本地文件");
            recordOperationLog(entity, file.toString());
            ArchiveResult res = new ArchiveResult();
            res.archivePath = file.toString();
            res.signature = signatureDbService.getById(entity.id);
            return res;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.SIGNATURE_ARCHIVE_FAILED, "SIG_ARCHIVE", "归档失败: " + ex.getMessage());
        }
    }

    private SignatureEntity mustGet(Long signatureId) {
        if (signatureId == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "SIG_INVALID", "signatureId 不能为空");
        }
        SignatureEntity entity = signatureDbService.getById(signatureId);
        if (entity == null) {
            throw new ApiException(ErrorCode.SIGNATURE_NOT_FOUND, "SIG_NONE", "签名记录不存在");
        }
        return entity;
    }

    private static void validateSignedFields(SignatureEntity entity) {
        if (entity.signCode == null || entity.signCode.isBlank()
                || entity.interviewId == null
                || entity.signUserId == null
                || entity.signUserName == null || entity.signUserName.isBlank()
                || entity.signType == null
                || entity.signImgUrl == null || entity.signImgUrl.isBlank()
                || entity.signTime == null
                || entity.signIp == null || entity.signIp.isBlank()) {
            throw new ApiException(ErrorCode.SIGNATURE_VERIFY_FAILED, "SIG_FIELDS", "签名字段不完整");
        }
    }

    private static String toSignPayload(SignatureEntity entity) {
        String evaluateId = entity.evaluateId == null ? "" : String.valueOf(entity.evaluateId);
        String signDevice = entity.signDevice == null ? "" : entity.signDevice;
        String certNo = entity.certificateNo == null ? "" : entity.certificateNo;
        return String.join("|",
                n(entity.signCode),
                n(entity.interviewId),
                evaluateId,
                n(entity.signUserId),
                n(entity.signUserName),
                n(entity.signType),
                n(entity.signImgUrl),
                n(entity.signTime),
                n(entity.signIp),
                signDevice,
                certNo
        );
    }

    private static String n(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static Map<String, Object> toArchiveDoc(SignatureEntity entity) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", entity.id);
        doc.put("signCode", entity.signCode);
        doc.put("interviewId", entity.interviewId);
        doc.put("evaluateId", entity.evaluateId);
        doc.put("signUserId", entity.signUserId);
        doc.put("signUserName", entity.signUserName);
        doc.put("signType", entity.signType);
        doc.put("signImgUrl", entity.signImgUrl);
        doc.put("signTime", entity.signTime == null ? null : entity.signTime.toString());
        doc.put("signIp", entity.signIp);
        doc.put("signDevice", entity.signDevice);
        doc.put("fileHash", entity.fileHash);
        doc.put("certificateNo", entity.certificateNo);
        doc.put("status", entity.status);
        doc.put("archivedAt", LocalDateTime.now().toString());
        return doc;
    }

    private void recordOperationLog(SignatureEntity entity, String archivePath) {
        OperationLogEntity log = new OperationLogEntity();
        log.logCode = "LOG" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.traceId = TraceContext.getTraceId();
        log.bizCode = entity.signCode;
        log.errorCode = "0";
        log.userId = entity.signUserId;
        log.userName = entity.signUserName;
        log.operationModule = "signature";
        log.operationType = "archive";
        log.operationDesc = "签名归档";
        log.requestUrl = "/api/v2/signatures/" + entity.id + "/archive";
        log.requestMethod = "POST";
        log.requestParams = "{\"signatureId\":" + entity.id + "}";
        log.responseResult = "{\"archivePath\":\"" + archivePath.replace("\"", "'") + "\"}";
        log.ipAddress = entity.signIp;
        log.deviceInfo = entity.signDevice;
        log.operationTime = LocalDateTime.now();
        log.costTime = 0;
        log.operationStatus = 1;
        log.failReason = null;
        log.createTime = LocalDateTime.now();
        auditLogDbService.create(log);
    }

    public static class VerifyResult {
        public boolean verified;
        public SignatureEntity signature;
    }

    public static class ArchiveResult {
        public String archivePath;
        public SignatureEntity signature;
    }
}

