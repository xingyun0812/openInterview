package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_user")
public class SysUserEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String username;
    public String password;
    public String realName;
    public String phone;
    public String email;
    public Long deptId;
    public Long roleId;
    public Integer status;
    public LocalDateTime lastLoginTime;
    public String lastLoginIp;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
