package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("interview_score_detail")
public class ScoreDetailEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long evaluateId;
    public Long interviewId;
    public Long itemId;
    public String itemName;
    public Integer itemFullScore;
    public BigDecimal score;
    public String itemComment;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
