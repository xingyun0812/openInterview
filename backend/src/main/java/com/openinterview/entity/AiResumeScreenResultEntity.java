package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ai_resume_screen_result")
public class AiResumeScreenResultEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long candidateId;
    public String jobCode;
    public Integer screenStatus;
    public BigDecimal matchScore;
    public Integer recommendLevel;
    public Integer reviewResult;
    public Long reviewUserId;
    public LocalDateTime reviewTime;
    public String reviewComment;
    public String coreReason;
    public String missingSkills;
    public String riskFlags;
    public String inputSnapshotHash;
    public String modelName;
    public String failReason;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
