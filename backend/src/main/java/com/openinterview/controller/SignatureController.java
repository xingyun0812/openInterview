package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.entity.SignatureEntity;
import com.openinterview.service.SignatureService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v2/signatures")
public class SignatureController {
    private final SignatureService signatureService;

    public SignatureController(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody @Validated GenerateRequest request) {
        SignatureEntity entity = signatureService.generate(
                request.interviewId, request.signUserId, request.signUserName, request.signType
        );
        Map<String, Object> data = new HashMap<>();
        data.put("signatureId", entity.id);
        data.put("signCode", entity.signCode);
        data.put("status", entity.status);
        return Result.success(data, TraceContext.getTraceId(), entity.signCode);
    }

    @PostMapping("/{id}/sign")
    public Result<Map<String, Object>> sign(@PathVariable("id") Long id,
                                           @RequestBody @Validated SignRequest request) {
        SignatureEntity entity = signatureService.sign(id, request.signImgUrl, request.signIp, request.deviceInfo);
        Map<String, Object> data = new HashMap<>();
        data.put("signatureId", entity.id);
        data.put("fileHash", entity.fileHash);
        data.put("status", entity.status);
        return Result.success(data, TraceContext.getTraceId(), entity.signCode);
    }

    @GetMapping("/{id}/verify")
    public Result<Map<String, Object>> verify(@PathVariable("id") Long id) {
        SignatureService.VerifyResult res = signatureService.verify(id);
        Map<String, Object> data = new HashMap<>();
        data.put("signatureId", res.signature.id);
        data.put("verified", res.verified);
        data.put("status", res.signature.status);
        return Result.success(data, TraceContext.getTraceId(), res.signature.signCode);
    }

    @PostMapping("/{id}/archive")
    public Result<Map<String, Object>> archive(@PathVariable("id") Long id) {
        SignatureService.ArchiveResult res = signatureService.archive(id);
        Map<String, Object> data = new HashMap<>();
        data.put("signatureId", res.signature.id);
        data.put("archivePath", res.archivePath);
        data.put("status", res.signature.status);
        return Result.success(data, TraceContext.getTraceId(), res.signature.signCode);
    }

    public static class GenerateRequest {
        @NotNull
        public Long interviewId;
        @NotNull
        public Long signUserId;
        @NotBlank
        public String signUserName;
        @NotNull
        public Integer signType;
    }

    public static class SignRequest {
        @NotBlank
        public String signImgUrl;
        @NotBlank
        public String signIp;
        public String deviceInfo;
    }
}

