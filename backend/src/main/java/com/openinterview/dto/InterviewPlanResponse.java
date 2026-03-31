package com.openinterview.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InterviewPlanResponse {
    public Long id;
    public String interviewCode;
    public Long candidateId;
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
    public Integer interviewStatus;
    public Integer interviewResult;
    public BigDecimal finalScore;
    public Integer isSigned;
    public String recordFileUrl;
    public String remark;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
}
