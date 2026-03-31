package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_role_menu")
public class SysRoleMenuEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long roleId;
    public Long menuId;
    public LocalDateTime createTime;
    @TableLogic
    public Integer isDeleted;
}
