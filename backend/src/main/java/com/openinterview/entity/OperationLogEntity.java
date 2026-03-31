package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * DDL 无 is_deleted 字段，不使用逻辑删除。
 */
@TableName("sys_operation_log")
public class OperationLogEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String logCode;
    public String traceId;
    public String bizCode;
    public String errorCode;
    public Long userId;
    public String userName;
    public String operationModule;
    public String operationType;
    public String operationDesc;
    public String requestUrl;
    public String requestMethod;
    public String requestParams;
    public String responseResult;
    public String ipAddress;
    public String deviceInfo;
    public LocalDateTime operationTime;
    public Integer costTime;
    public Integer operationStatus;
    public String failReason;
    public LocalDateTime createTime;
}
