package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_menu")
public class SysMenuEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public Long parentId;
    public String menuName;
    public String menuCode;
    public Integer menuType;
    public String path;
    public String icon;
    public Integer sort;
    public Integer status;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
