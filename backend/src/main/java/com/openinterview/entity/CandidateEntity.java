package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("interview_candidate")
public class CandidateEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String candidateCode;
    public String name;
    public String phone;
    public String email;
    public String idCard;
    public Integer gender;
    public Integer age;
    public String applyPosition;
    public String workYears;
    public String highestEducation;
    public String resumeUrl;
    public String source;
    public Integer status;
    public Long createUser;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
