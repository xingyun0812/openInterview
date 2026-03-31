package com.openinterview.controller;

import com.openinterview.common.Result;
import com.openinterview.dto.InterviewPlanCreateRequest;
import com.openinterview.dto.InterviewPlanPageResponse;
import com.openinterview.dto.InterviewPlanResponse;
import com.openinterview.dto.InterviewPlanUpdateRequest;
import com.openinterview.service.InterviewPlanService;
import com.openinterview.trace.TraceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/interview-plans")
public class InterviewPlanController {

    private final InterviewPlanService interviewPlanService;

    public InterviewPlanController(InterviewPlanService interviewPlanService) {
        this.interviewPlanService = interviewPlanService;
    }

    @PostMapping
    public ResponseEntity<Result<InterviewPlanResponse>> create(
            @RequestBody @Valid InterviewPlanCreateRequest request,
            @RequestHeader("X-Idempotency-Key") String idemKey) {
        InterviewPlanResponse data = interviewPlanService.create(request, idemKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(data, TraceContext.getTraceId(), data.interviewCode));
    }

    @GetMapping("/{id}")
    public Result<InterviewPlanResponse> getById(@PathVariable("id") Long id) {
        InterviewPlanResponse data = interviewPlanService.getById(id);
        return Result.success(data, TraceContext.getTraceId(), data.interviewCode);
    }

    @GetMapping("/code/{code}")
    public Result<InterviewPlanResponse> getByCode(@PathVariable("code") String code) {
        InterviewPlanResponse data = interviewPlanService.getByInterviewCode(code);
        return Result.success(data, TraceContext.getTraceId(), data.interviewCode);
    }

    @GetMapping
    public Result<InterviewPlanPageResponse> list(
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "10") long size,
            @RequestParam(name = "status", required = false) Integer status) {
        InterviewPlanPageResponse data = interviewPlanService.page(page, size, status);
        return Result.success(data, TraceContext.getTraceId(), "INT_PLAN_LIST");
    }

    @PutMapping("/{id}")
    public Result<InterviewPlanResponse> update(
            @PathVariable("id") Long id,
            @RequestBody InterviewPlanUpdateRequest request) {
        InterviewPlanResponse data = interviewPlanService.update(id, request);
        return Result.success(data, TraceContext.getTraceId(), data.interviewCode);
    }

    @PostMapping("/{id}/start")
    public Result<InterviewPlanResponse> start(@PathVariable("id") Long id) {
        InterviewPlanResponse data = interviewPlanService.start(id);
        return Result.success(data, TraceContext.getTraceId(), data.interviewCode);
    }

    @PostMapping("/{id}/complete")
    public Result<InterviewPlanResponse> complete(@PathVariable("id") Long id) {
        InterviewPlanResponse data = interviewPlanService.complete(id);
        return Result.success(data, TraceContext.getTraceId(), data.interviewCode);
    }

    @PostMapping("/{id}/cancel")
    public Result<InterviewPlanResponse> cancel(@PathVariable("id") Long id) {
        InterviewPlanResponse data = interviewPlanService.cancel(id);
        return Result.success(data, TraceContext.getTraceId(), data.interviewCode);
    }
}
