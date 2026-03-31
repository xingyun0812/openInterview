package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("interview_signature")
public class SignatureEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String signCode;
    public Long interviewId;
    public Long evaluateId;
    public Long signUserId;
    public String signUserName;
    public Integer signType;
    public String signImgUrl;
    public LocalDateTime signTime;
    public String signIp;
    public String signDevice;
    public String fileHash;
    public String certificateNo;
    public Integer status;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
