package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_resume_parse_result")
public class AiResumeParseResultEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long candidateId;
    public String resumeFileUrl;
    public Integer parseStatus;
    public String basicInfoJson;
    public String educationJson;
    public String workExperienceJson;
    public String projectJson;
    public String skillTagsJson;
    public String rawText;
    public String failReason;
    public String modelName;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
