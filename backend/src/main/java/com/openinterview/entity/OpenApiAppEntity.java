package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("open_api_app")
public class OpenApiAppEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String appId;
    public String appSecret;
    public String appName;
    public String appDesc;
    public String apiPermissions;
    public String webhookUrl;
    public String webhookSecret;
    public Integer status;
    public Long createUser;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
