package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("interview_score_item")
public class ScoreItemEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long templateId;
    public String itemName;
    public String itemDesc;
    public Integer fullScore;
    public BigDecimal weight;
    public Integer sort;
    public Integer isRequired;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
