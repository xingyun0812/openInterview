package com.openinterview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class InterviewPlanCreateRequest {
    @NotNull
    public Long candidateId;
    @NotBlank
    public String applyPosition;
    @NotBlank
    public String interviewRound;
    @NotNull
    public Integer interviewType;
    @NotNull
    public Long templateId;
    @NotNull
    @Future
    public LocalDateTime interviewStartTime;
    @NotNull
    public LocalDateTime interviewEndTime;
    public String interviewRoomId;
    public String interviewRoomLink;
    public Long hrUserId;
    public String interviewerIds;
}
