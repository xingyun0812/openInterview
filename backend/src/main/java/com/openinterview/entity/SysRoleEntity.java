package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_role")
public class SysRoleEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String roleName;
    public String roleCode;
    public String description;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
