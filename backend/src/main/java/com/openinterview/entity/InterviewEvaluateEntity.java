package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("interview_evaluate")
public class InterviewEvaluateEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long interviewId;
    public Long interviewerId;
    public String interviewerName;
    public BigDecimal totalScore;
    public Integer interviewResult;
    public String advantageComment;
    public String disadvantageComment;
    public String comprehensiveComment;
    public Integer submitStatus;
    public LocalDateTime submitTime;
    public Integer isSigned;
    public LocalDateTime signTime;
    public String signImgUrl;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
