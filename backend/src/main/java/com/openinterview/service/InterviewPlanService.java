package com.openinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.dto.InterviewPlanCreateRequest;
import com.openinterview.dto.InterviewPlanPageResponse;
import com.openinterview.dto.InterviewPlanResponse;
import com.openinterview.dto.InterviewPlanUpdateRequest;
import com.openinterview.entity.InterviewPlanEntity;
import com.openinterview.service.db.InterviewPlanDbService;
import com.openinterview.statemachine.InterviewPlanStateMachine;
import com.openinterview.statemachine.InterviewStatusMachine;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class InterviewPlanService {

    private static final int PENDING = InterviewPlanStateMachine.PENDING.getCode();

    private final InterviewPlanDbService interviewPlanDbService;
    private final ConcurrentHashMap<String, Long> idempotencyCreateIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> createLocks = new ConcurrentHashMap<>();

    public InterviewPlanService(InterviewPlanDbService interviewPlanDbService) {
        this.interviewPlanDbService = interviewPlanDbService;
    }

    private Object lockFor(String key) {
        return createLocks.computeIfAbsent(key, k -> new Object());
    }

    public InterviewPlanResponse create(InterviewPlanCreateRequest req, String idemKey) {
        validateTimeRange(req.interviewStartTime, req.interviewEndTime);
        synchronized (lockFor(idemKey)) {
            Long existingId = idempotencyCreateIndex.get(idemKey);
            if (existingId != null) {
                InterviewPlanEntity cached = interviewPlanDbService.getById(existingId);
                if (cached != null) {
                    return toResponse(cached);
                }
            }
            InterviewPlanEntity e = new InterviewPlanEntity();
            e.interviewCode = generateInterviewCode();
            e.candidateId = req.candidateId;
            e.applyPosition = req.applyPosition;
            e.interviewRound = req.interviewRound;
            e.interviewType = req.interviewType;
            e.templateId = req.templateId;
            e.interviewStartTime = req.interviewStartTime;
            e.interviewEndTime = req.interviewEndTime;
            e.interviewRoomId = blankToDefault(req.interviewRoomId);
            e.interviewRoomLink = blankToDefault(req.interviewRoomLink);
            e.hrUserId = req.hrUserId != null ? req.hrUserId : 0L;
            e.interviewerIds = blankToDefault(req.interviewerIds);
            e.interviewStatus = PENDING;
            e.isSigned = 0;
            e.isDeleted = 0;

            InterviewPlanEntity saved = interviewPlanDbService.create(e);
            idempotencyCreateIndex.put(idemKey, saved.id);
            return toResponse(saved);
        }
    }

    public InterviewPlanResponse getById(Long id) {
        InterviewPlanEntity e = interviewPlanDbService.getById(id);
        if (e == null) {
            throw new ApiException(ErrorCode.INTERVIEW_NOT_FOUND, "INT_PLAN_NONE", "面试计划不存在");
        }
        return toResponse(e);
    }

    public InterviewPlanResponse getByInterviewCode(String code) {
        InterviewPlanEntity e = interviewPlanDbService.getByInterviewCode(code);
        if (e == null) {
            throw new ApiException(ErrorCode.INTERVIEW_NOT_FOUND, "INT_PLAN_NONE", "面试计划不存在");
        }
        return toResponse(e);
    }

    public InterviewPlanPageResponse page(long current, long size, Integer status) {
        IPage<InterviewPlanEntity> p = interviewPlanDbService.page(current, size, status);
        InterviewPlanPageResponse out = new InterviewPlanPageResponse();
        out.records = p.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        out.total = p.getTotal();
        out.current = p.getCurrent();
        out.size = p.getSize();
        return out;
    }

    public InterviewPlanResponse update(Long id, InterviewPlanUpdateRequest req) {
        InterviewPlanEntity e = interviewPlanDbService.getById(id);
        if (e == null) {
            throw new ApiException(ErrorCode.INTERVIEW_NOT_FOUND, "INT_PLAN_NONE", "面试计划不存在");
        }
        if (e.interviewStatus == null || e.interviewStatus != PENDING) {
            throw new ApiException(ErrorCode.INTERVIEW_NOT_EDITABLE, e.interviewCode, "面试计划仅待面试状态可编辑");
        }
        if (req.applyPosition != null) {
            e.applyPosition = req.applyPosition;
        }
        if (req.interviewRound != null) {
            e.interviewRound = req.interviewRound;
        }
        if (req.interviewType != null) {
            e.interviewType = req.interviewType;
        }
        if (req.templateId != null) {
            e.templateId = req.templateId;
        }
        if (req.interviewStartTime != null) {
            e.interviewStartTime = req.interviewStartTime;
        }
        if (req.interviewEndTime != null) {
            e.interviewEndTime = req.interviewEndTime;
        }
        if (req.interviewRoomId != null) {
            e.interviewRoomId = req.interviewRoomId;
        }
        if (req.interviewRoomLink != null) {
            e.interviewRoomLink = req.interviewRoomLink;
        }
        if (req.hrUserId != null) {
            e.hrUserId = req.hrUserId;
        }
        if (req.interviewerIds != null) {
            e.interviewerIds = req.interviewerIds;
        }
        if (req.remark != null) {
            e.remark = req.remark;
        }
        if (e.interviewStartTime != null && e.interviewEndTime != null
                && !e.interviewEndTime.isAfter(e.interviewStartTime)) {
            throw new ApiException(ErrorCode.PARAM_INVALID, e.interviewCode, "结束时间必须晚于开始时间");
        }
        interviewPlanDbService.updateById(e);
        return toResponse(interviewPlanDbService.getById(id));
    }

    public InterviewPlanResponse start(Long id) {
        InterviewPlanEntity e = requirePlan(id);
        int target = InterviewPlanStateMachine.IN_PROGRESS.getCode();
        InterviewStatusMachine.transit(e, target);
        e.interviewStatus = target;
        interviewPlanDbService.updateById(e);
        return toResponse(interviewPlanDbService.getById(id));
    }

    public InterviewPlanResponse complete(Long id) {
        InterviewPlanEntity e = requirePlan(id);
        int target = InterviewPlanStateMachine.COMPLETED.getCode();
        InterviewStatusMachine.transit(e, target);
        e.interviewStatus = target;
        interviewPlanDbService.updateById(e);
        return toResponse(interviewPlanDbService.getById(id));
    }

    public InterviewPlanResponse cancel(Long id) {
        InterviewPlanEntity e = requirePlan(id);
        int target = InterviewPlanStateMachine.CANCELLED.getCode();
        InterviewStatusMachine.transit(e, target);
        e.interviewStatus = target;
        interviewPlanDbService.updateById(e);
        return toResponse(interviewPlanDbService.getById(id));
    }

    private InterviewPlanEntity requirePlan(Long id) {
        InterviewPlanEntity e = interviewPlanDbService.getById(id);
        if (e == null) {
            throw new ApiException(ErrorCode.INTERVIEW_NOT_FOUND, "INT_PLAN_NONE", "面试计划不存在");
        }
        return e;
    }

    private void validateTimeRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "INT_TIME", "面试时间不能为空");
        }
        if (!end.isAfter(start)) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "INT_TIME", "结束时间必须晚于开始时间");
        }
    }

    private String generateInterviewCode() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 16; i++) {
            String code = "INT" + java.time.LocalDateTime.now().format(fmt)
                    + String.format("%04d", r.nextInt(10000));
            if (interviewPlanDbService.getByInterviewCode(code) == null) {
                return code;
            }
        }
        throw new ApiException(ErrorCode.SYSTEM_ERROR, "INT_CODE", "生成面试编码失败");
    }

    private static String blankToDefault(String s) {
        return s == null || s.isBlank() ? "" : s;
    }

    private InterviewPlanResponse toResponse(InterviewPlanEntity e) {
        InterviewPlanResponse r = new InterviewPlanResponse();
        r.id = e.id;
        r.interviewCode = e.interviewCode;
        r.candidateId = e.candidateId;
        r.applyPosition = e.applyPosition;
        r.interviewRound = e.interviewRound;
        r.interviewType = e.interviewType;
        r.templateId = e.templateId;
        r.interviewStartTime = e.interviewStartTime;
        r.interviewEndTime = e.interviewEndTime;
        r.interviewRoomId = e.interviewRoomId;
        r.interviewRoomLink = e.interviewRoomLink;
        r.hrUserId = e.hrUserId;
        r.interviewerIds = e.interviewerIds;
        r.interviewStatus = e.interviewStatus;
        r.interviewResult = e.interviewResult;
        r.finalScore = e.finalScore;
        r.isSigned = e.isSigned;
        r.recordFileUrl = e.recordFileUrl;
        r.remark = e.remark;
        r.createTime = e.createTime;
        r.updateTime = e.updateTime;
        return r;
    }
}
