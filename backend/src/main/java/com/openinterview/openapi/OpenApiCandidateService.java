package com.openinterview.openapi;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.entity.CandidateEntity;
import com.openinterview.service.db.CandidateDbService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class OpenApiCandidateService {
    private final CandidateDbService candidateDbService;

    public OpenApiCandidateService(CandidateDbService candidateDbService) {
        this.candidateDbService = candidateDbService;
    }

    public CandidateEntity create(OpenApiDtos.OpenApiPushCandidateRequest req) {
        if (req == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "OPEN_API_CAND", "请求体不能为空");
        }
        if (req.name == null || req.name.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "OPEN_API_CAND", "name 不能为空");
        }
        if (req.phone == null || req.phone.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "OPEN_API_CAND", "phone 不能为空");
        }
        if (req.applyPosition == null || req.applyPosition.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "OPEN_API_CAND", "applyPosition 不能为空");
        }

        CandidateEntity e = new CandidateEntity();
        e.candidateCode = generateCandidateCode();
        e.name = req.name.trim();
        e.phone = req.phone.trim();
        e.email = req.email == null ? null : req.email.trim();
        e.applyPosition = req.applyPosition.trim();
        e.resumeUrl = req.resumeUrl == null ? null : req.resumeUrl.trim();
        e.source = req.source == null ? "OPEN_API" : req.source.trim();
        e.status = 1;
        e.createUser = 0L;
        LocalDateTime now = LocalDateTime.now();
        e.createTime = now;
        e.updateTime = now;
        e.isDeleted = 0;
        candidateDbService.create(e);
        return e;
    }

    private String generateCandidateCode() {
        String t = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String r = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "CAND" + t + r;
    }
}

