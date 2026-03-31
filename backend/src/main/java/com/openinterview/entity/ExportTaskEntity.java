package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("interview_export_task")
public class ExportTaskEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String taskCode;
    public Integer exportType;
    public String exportContent;
    public String jobCode;
    public Long templateId;
    public Long exportUserId;
    public String exportUserName;
    public Integer taskStatus;
    public String fileUrl;
    public String fileName;
    public Long fileSize;
    public String fileHash;
    public String failReason;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
