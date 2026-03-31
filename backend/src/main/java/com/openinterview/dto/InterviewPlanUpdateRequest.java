package com.openinterview.dto;

import java.time.LocalDateTime;

/**
 * 全部可选；仅 {@code PENDING} 状态可更新。
 */
public class InterviewPlanUpdateRequest {
    public String applyPosition;
    public String interviewRound;
    public Integer interviewType;
    public Long templateId;
    public LocalDateTime interviewStartTime;
    public LocalDateTime interviewEndTime;
    public String interviewRoomId;
    public String interviewRoomLink;
    public Long hrUserId;
    public String interviewerIds;
    public String remark;
}
