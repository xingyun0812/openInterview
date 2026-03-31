package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("interview_score_template")
public class ScoreTemplateEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String templateName;
    public String templateCode;
    public String applyPositionType;
    public String interviewRound;
    public Integer fullScore;
    public Integer passScore;
    public Integer scoreRule;
    public String description;
    public Integer status;
    public Long createUser;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
