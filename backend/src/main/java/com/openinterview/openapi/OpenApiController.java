package com.openinterview.openapi;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.common.Result;
import com.openinterview.entity.CandidateEntity;
import com.openinterview.entity.OpenApiAppEntity;
import com.openinterview.service.EvaluationService;
import com.openinterview.service.InterviewPlanService;
import com.openinterview.trace.TraceContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/open-api")
@Tag(name = "Open API")
public class OpenApiController {
    private final OpenApiAppService openApiAppService;
    private final OpenApiCandidateService openApiCandidateService;
    private final InterviewPlanService interviewPlanService;
    private final EvaluationService evaluationService;

    public OpenApiController(OpenApiAppService openApiAppService,
                             OpenApiCandidateService openApiCandidateService,
                             InterviewPlanService interviewPlanService,
                             EvaluationService evaluationService) {
        this.openApiAppService = openApiAppService;
        this.openApiCandidateService = openApiCandidateService;
        this.interviewPlanService = interviewPlanService;
        this.evaluationService = evaluationService;
    }

    @PostMapping("/candidates/push")
    @Operation(summary = "外部推送候选人", description = "写入 interview_candidate，返回 candidateCode")
    public Result<OpenApiDtos.OpenApiPushCandidateResponse> pushCandidate(
            @RequestBody @Valid OpenApiDtos.OpenApiPushCandidateRequest req,
            HttpServletRequest servletRequest) {
        OpenApiAppEntity app = currentApp(servletRequest);
        openApiAppService.requireScope(app, "candidates:push");
        CandidateEntity saved = openApiCandidateService.create(req);
        OpenApiDtos.OpenApiPushCandidateResponse out = new OpenApiDtos.OpenApiPushCandidateResponse();
        out.candidateId = saved.id;
        out.candidateCode = saved.candidateCode;
        return Result.success(out, TraceContext.getTraceId(), saved.candidateCode);
    }

    @GetMapping("/interviews/{interviewCode}/result")
    @Operation(summary = "查询面试结果", description = "返回面试状态 + 汇总信息（最小可用结构）")
    public Result<OpenApiDtos.OpenApiInterviewResultResponse> interviewResult(
            @Parameter(description = "面试编码", example = "INT202603311234560001")
            @PathVariable("interviewCode") String interviewCode,
            HttpServletRequest servletRequest) {
        OpenApiAppEntity app = currentApp(servletRequest);
        openApiAppService.requireScope(app, "interviews:result:read");

        var plan = interviewPlanService.getByInterviewCode(interviewCode);
        List<EvaluationService.EvaluationView> evals = evaluationService.listEvaluations(plan.id);
        OpenApiDtos.OpenApiInterviewResultResponse out = new OpenApiDtos.OpenApiInterviewResultResponse();
        out.interviewCode = plan.interviewCode;
        out.interviewStatus = plan.interviewStatus;
        out.interviewResult = plan.interviewResult;
        out.finalScore = plan.finalScore;
        out.flowStatus = guessFlowStatus(plan.finalScore, plan.interviewResult, evals);
        out.evaluations = evals.stream().map(e -> {
            OpenApiDtos.EvaluationSummary s = new OpenApiDtos.EvaluationSummary();
            s.evaluateId = e.evaluateId;
            s.interviewerId = e.interviewerId;
            s.interviewerName = e.interviewerName;
            s.totalScore = e.totalScore;
            s.interviewResult = e.interviewResult;
            s.submitStatus = e.submitStatus;
            s.submitTime = e.submitTime;
            return s;
        }).collect(Collectors.toList());
        return Result.success(out, TraceContext.getTraceId(), plan.interviewCode);
    }

    @PostMapping("/webhooks/subscribe")
    @Operation(summary = "注册 Webhook", description = "写 open_api_app.webhook_url / webhook_secret")
    public Result<OpenApiDtos.OpenApiSubscribeWebhookResponse> subscribeWebhook(
            @RequestBody @Valid OpenApiDtos.OpenApiSubscribeWebhookRequest req,
            HttpServletRequest servletRequest) {
        OpenApiAppEntity app = currentApp(servletRequest);
        openApiAppService.requireScope(app, "webhooks:subscribe");
        openApiAppService.updateWebhook(app, req.webhookUrl.trim(), req.webhookSecret.trim());
        OpenApiDtos.OpenApiSubscribeWebhookResponse out = new OpenApiDtos.OpenApiSubscribeWebhookResponse();
        out.appId = app.appId;
        out.webhookUrl = app.webhookUrl;
        return Result.success(out, TraceContext.getTraceId(), app.appId);
    }

    private OpenApiAppEntity currentApp(HttpServletRequest request) {
        Object v = request.getAttribute(OpenApiConstants.REQ_ATTR_APP_ID);
        String appId = v == null ? null : String.valueOf(v);
        if (appId == null || appId.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "OPEN_API_APP", "缺少 AppId 上下文");
        }
        return openApiAppService.requireActiveApp(appId);
    }

    private static String guessFlowStatus(java.math.BigDecimal finalScore,
                                         Integer interviewResult,
                                         List<EvaluationService.EvaluationView> evals) {
        boolean anySubmitted = evals != null && evals.stream().anyMatch(e -> e.submitStatus != null && e.submitStatus == 1);
        if (!anySubmitted) {
            return "DRAFT";
        }
        if (finalScore == null) {
            return "SUBMITTED";
        }
        if (finalScore != null && interviewResult == null) {
            return "SUMMARIZED";
        }
        return "REVIEWED";
    }
}

