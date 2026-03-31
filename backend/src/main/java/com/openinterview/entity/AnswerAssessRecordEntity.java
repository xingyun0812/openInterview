package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("interview_answer_assess_record")
public class AnswerAssessRecordEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String recordCode;
    public Long interviewId;
    public Long questionId;
    public Long candidateId;
    public String answerText;
    public BigDecimal accuracyScore;
    public BigDecimal coverageScore;
    public BigDecimal clarityScore;
    public BigDecimal totalScore;
    public String followUpSuggest;
    public String modelName;
    public Long createUser;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
