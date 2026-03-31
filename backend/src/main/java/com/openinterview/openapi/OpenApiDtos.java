package com.openinterview.openapi;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OpenApiDtos {
    private OpenApiDtos() {
    }

    public static class OpenApiPushCandidateRequest {
        @Schema(description = "候选人姓名", example = "张三")
        @NotBlank
        public String name;

        @Schema(description = "手机号", example = "13800000000")
        @NotBlank
        public String phone;

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        public String email;

        @Schema(description = "应聘岗位", example = "Java 后端工程师")
        @NotBlank
        public String applyPosition;

        @Schema(description = "简历 URL", example = "https://example.com/resume.pdf")
        public String resumeUrl;

        @Schema(description = "来源", example = "BOSS")
        public String source;
    }

    public static class OpenApiPushCandidateResponse {
        @Schema(description = "candidateId")
        public Long candidateId;
        @Schema(description = "候选人编码", example = "CAND20260331123456ABCDEF")
        public String candidateCode;
    }

    public static class OpenApiSubscribeWebhookRequest {
        @Schema(description = "Webhook URL", example = "https://partner.example.com/openinterview/webhook")
        @NotBlank
        public String webhookUrl;

        @Schema(description = "Webhook Secret（用于对我方回调签名/验签）", example = "whsec_xxx")
        public String webhookSecret;
    }

    public static class OpenApiSubscribeWebhookResponse {
        public String appId;
        public String webhookUrl;
    }

    public static class OpenApiInterviewResultResponse {
        public String interviewCode;
        public Integer interviewStatus;
        public Integer interviewResult;
        public BigDecimal finalScore;
        public String flowStatus;
        public List<EvaluationSummary> evaluations;
    }

    public static class EvaluationSummary {
        public Long evaluateId;
        public Long interviewerId;
        public String interviewerName;
        public BigDecimal totalScore;
        public Integer interviewResult;
        public Integer submitStatus;
        public LocalDateTime submitTime;
    }
}

