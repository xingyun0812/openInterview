-- V20260327_01__init_core_tables.sql
-- Core schema initialization (RFC baseline v1 aligned)

CREATE DATABASE IF NOT EXISTS `interview_platform`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `interview_platform`;

-- 0. Dictionary table (for enum bootstrap)
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_type` varchar(64) NOT NULL COMMENT '字典类型',
  `dict_code` varchar(64) NOT NULL COMMENT '字典编码',
  `dict_name` varchar(128) NOT NULL COMMENT '字典名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_code` (`dict_type`,`dict_code`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统字典表';

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `real_name` varchar(32) NOT NULL COMMENT '真实姓名',
  `phone` varchar(16) DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(32) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(32) NOT NULL COMMENT '角色名称',
  `role_code` varchar(32) NOT NULL COMMENT '角色编码',
  `description` varchar(256) DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 3. 权限菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(32) NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(64) NOT NULL COMMENT '权限编码',
  `menu_type` tinyint NOT NULL COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `path` varchar(128) DEFAULT NULL COMMENT '路由路径',
  `icon` varchar(64) DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限菜单表';

-- 4. 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 5. 候选人信息表
CREATE TABLE IF NOT EXISTS `interview_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `candidate_code` varchar(32) NOT NULL COMMENT '候选人唯一编码',
  `name` varchar(32) NOT NULL COMMENT '姓名',
  `phone` varchar(16) NOT NULL COMMENT '手机号',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `id_card` varchar(32) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `gender` tinyint DEFAULT NULL COMMENT '性别 1-男 2-女',
  `age` int DEFAULT NULL COMMENT '年龄',
  `apply_position` varchar(64) NOT NULL COMMENT '应聘岗位',
  `work_years` varchar(16) DEFAULT NULL COMMENT '工作年限',
  `highest_education` varchar(16) DEFAULT NULL COMMENT '最高学历',
  `resume_url` varchar(256) DEFAULT NULL COMMENT '简历文件地址',
  `source` varchar(32) DEFAULT NULL COMMENT '简历来源',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '候选人状态 1-待面试 2-面试中 3-已通过 4-已淘汰 5-已待定',
  `create_user` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_candidate_code` (`candidate_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_apply_position` (`apply_position`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人信息表';

-- 6. 面试打分模板表
CREATE TABLE IF NOT EXISTS `interview_score_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(64) NOT NULL COMMENT '模板名称',
  `template_code` varchar(32) NOT NULL COMMENT '模板编码',
  `apply_position_type` varchar(32) DEFAULT NULL COMMENT '适用岗位类型',
  `interview_round` varchar(16) DEFAULT NULL COMMENT '适用面试轮次',
  `full_score` int NOT NULL DEFAULT '100' COMMENT '满分值',
  `pass_score` int NOT NULL DEFAULT '60' COMMENT '及格线',
  `score_rule` tinyint NOT NULL DEFAULT '1' COMMENT '汇总规则 1-平均分 2-加权分 3-去掉最高最低取平均',
  `description` varchar(256) DEFAULT NULL COMMENT '模板描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_user` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试打分模板表';

-- 7. 打分模板明细项表
CREATE TABLE IF NOT EXISTS `interview_score_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `item_name` varchar(64) NOT NULL COMMENT '打分项名称',
  `item_desc` varchar(256) DEFAULT NULL COMMENT '打分项说明/评分标准',
  `full_score` int NOT NULL COMMENT '该项满分值',
  `weight` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '权重',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_required` tinyint NOT NULL DEFAULT '1' COMMENT '是否必填 0-非必填 1-必填',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打分模板明细项表';

-- 8. 面试计划表
CREATE TABLE IF NOT EXISTS `interview_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `interview_code` varchar(32) NOT NULL COMMENT '面试唯一编码',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `apply_position` varchar(64) NOT NULL COMMENT '应聘岗位',
  `interview_round` varchar(16) NOT NULL COMMENT '面试轮次',
  `interview_type` tinyint NOT NULL COMMENT '面试类型 1-视频面试 2-电话面试 3-现场面试',
  `template_id` bigint NOT NULL COMMENT '打分模板ID',
  `interview_start_time` datetime NOT NULL COMMENT '面试开始时间',
  `interview_end_time` datetime NOT NULL COMMENT '面试结束时间',
  `interview_room_id` varchar(64) NOT NULL COMMENT '面试间ID',
  `interview_room_link` varchar(256) NOT NULL COMMENT '面试间链接',
  `hr_user_id` bigint NOT NULL COMMENT '负责HR用户ID',
  `interviewer_ids` varchar(512) NOT NULL COMMENT '面试官ID列表，逗号分隔',
  `interview_status` tinyint NOT NULL DEFAULT '1' COMMENT '面试状态 1-待面试 2-面试中 3-已完成 4-已取消 5-已逾期',
  `interview_result` tinyint DEFAULT NULL COMMENT '面试结果 1-通过 2-待定 3-淘汰',
  `final_score` decimal(5,2) DEFAULT NULL COMMENT '最终汇总得分',
  `is_signed` tinyint NOT NULL DEFAULT '0' COMMENT '是否完成签名 0-未完成 1-已完成',
  `record_file_url` varchar(256) DEFAULT NULL COMMENT '面试录制文件地址',
  `remark` varchar(512) DEFAULT NULL COMMENT 'HR备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_code` (`interview_code`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_hr_user_id` (`hr_user_id`),
  KEY `idx_interview_status` (`interview_status`),
  KEY `idx_interview_time` (`interview_start_time`,`interview_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试计划表';

-- 9. 面试官打分评价表
CREATE TABLE IF NOT EXISTS `interview_evaluate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `interview_id` bigint NOT NULL COMMENT '面试计划ID',
  `interviewer_id` bigint NOT NULL COMMENT '面试官用户ID',
  `interviewer_name` varchar(32) NOT NULL COMMENT '面试官姓名',
  `total_score` decimal(5,2) NOT NULL COMMENT '本次打分总分',
  `interview_result` tinyint NOT NULL COMMENT '面试官建议 1-通过 2-待定 3-淘汰',
  `advantage_comment` text COMMENT '优点评价',
  `disadvantage_comment` text COMMENT '不足评价',
  `comprehensive_comment` text NOT NULL COMMENT '综合评价',
  `submit_status` tinyint NOT NULL DEFAULT '0' COMMENT '提交状态 0-暂存 1-已提交',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `is_signed` tinyint NOT NULL DEFAULT '0' COMMENT '是否已签名 0-未签名 1-已签名',
  `sign_time` datetime DEFAULT NULL COMMENT '签名时间',
  `sign_img_url` varchar(256) DEFAULT NULL COMMENT '签名图片地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_interviewer` (`interview_id`,`interviewer_id`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_interviewer_id` (`interviewer_id`),
  KEY `idx_submit_status` (`submit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试官打分评价表';

-- 10. 打分明细记录表
CREATE TABLE IF NOT EXISTS `interview_score_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluate_id` bigint NOT NULL COMMENT '评价表ID',
  `interview_id` bigint NOT NULL COMMENT '面试计划ID',
  `item_id` bigint NOT NULL COMMENT '打分项ID',
  `item_name` varchar(64) NOT NULL COMMENT '打分项名称',
  `item_full_score` int NOT NULL COMMENT '打分项满分',
  `score` decimal(5,2) NOT NULL COMMENT '实际打分',
  `item_comment` varchar(256) DEFAULT NULL COMMENT '单项备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_evaluate_item` (`evaluate_id`,`item_id`),
  KEY `idx_evaluate_id` (`evaluate_id`),
  KEY `idx_interview_id` (`interview_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打分明细记录表';

-- 11. 电子签名表
CREATE TABLE IF NOT EXISTS `interview_signature` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sign_code` varchar(32) NOT NULL COMMENT '签名唯一编码',
  `interview_id` bigint NOT NULL COMMENT '面试计划ID',
  `evaluate_id` bigint DEFAULT NULL COMMENT '评价表ID',
  `sign_user_id` bigint NOT NULL COMMENT '签名人用户ID',
  `sign_user_name` varchar(32) NOT NULL COMMENT '签名人姓名',
  `sign_type` tinyint NOT NULL COMMENT '签名类型 1-面试官评价签名 2-HR复核签名',
  `sign_img_url` varchar(256) NOT NULL COMMENT '签名图片地址',
  `sign_time` datetime NOT NULL COMMENT '签名时间',
  `sign_ip` varchar(32) NOT NULL COMMENT '签名IP地址',
  `sign_device` varchar(128) DEFAULT NULL COMMENT '签名设备信息',
  `file_hash` varchar(64) NOT NULL COMMENT '签名文件哈希值',
  `certificate_no` varchar(64) DEFAULT NULL COMMENT '第三方签名证书编号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '签名状态 0-失效 1-有效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sign_code` (`sign_code`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_evaluate_id` (`evaluate_id`),
  KEY `idx_sign_user_id` (`sign_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签名表';

-- 12. 导出任务表（RFC-2）
CREATE TABLE IF NOT EXISTS `interview_export_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_code` varchar(32) NOT NULL COMMENT '导出任务编码',
  `export_type` tinyint NOT NULL COMMENT '导出类型 0-筛选Excel 1-成绩Excel 2-面试Word',
  `export_content` varchar(512) NOT NULL COMMENT '导出内容（面试ID或候选人ID列表，逗号分隔）',
  `job_code` varchar(64) DEFAULT NULL COMMENT '岗位编码（筛选导出时必填）',
  `template_id` bigint DEFAULT NULL COMMENT '自定义模板ID',
  `export_user_id` bigint NOT NULL COMMENT '导出人用户ID',
  `export_user_name` varchar(32) NOT NULL COMMENT '导出人姓名',
  `task_status` tinyint NOT NULL DEFAULT '1' COMMENT '任务状态 1-处理中 2-已完成 3-失败',
  `file_url` varchar(256) DEFAULT NULL COMMENT '导出文件下载地址',
  `file_name` varchar(128) DEFAULT NULL COMMENT '导出文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_hash` varchar(64) DEFAULT NULL COMMENT '导出文件SHA256',
  `fail_reason` varchar(256) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_export_user_id` (`export_user_id`),
  KEY `idx_task_status` (`task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务表';

-- 13. 操作审计日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `log_code` varchar(32) NOT NULL COMMENT '日志唯一编码',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `biz_code` varchar(64) DEFAULT NULL COMMENT '业务编码',
  `error_code` varchar(32) DEFAULT NULL COMMENT '错误码',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `user_name` varchar(32) DEFAULT NULL COMMENT '操作用户姓名',
  `operation_module` varchar(32) NOT NULL COMMENT '操作模块',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型',
  `operation_desc` varchar(256) NOT NULL COMMENT '操作描述',
  `request_url` varchar(256) DEFAULT NULL COMMENT '请求地址',
  `request_method` varchar(16) DEFAULT NULL COMMENT '请求方式',
  `request_params` text COMMENT '请求参数',
  `response_result` text COMMENT '响应结果',
  `ip_address` varchar(32) DEFAULT NULL COMMENT '操作IP',
  `device_info` varchar(256) DEFAULT NULL COMMENT '设备信息',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  `cost_time` int NOT NULL COMMENT '耗时（毫秒）',
  `operation_status` tinyint NOT NULL COMMENT '操作状态 0-失败 1-成功',
  `fail_reason` varchar(256) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation_module` (`operation_module`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

-- 14. 开放平台应用表
CREATE TABLE IF NOT EXISTS `open_api_app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` varchar(32) NOT NULL COMMENT '应用唯一ID',
  `app_secret` varchar(64) NOT NULL COMMENT '应用密钥（加密存储）',
  `app_name` varchar(64) NOT NULL COMMENT '应用名称',
  `app_desc` varchar(256) DEFAULT NULL COMMENT '应用描述',
  `api_permissions` varchar(1024) DEFAULT NULL COMMENT 'API权限列表',
  `webhook_url` varchar(256) DEFAULT NULL COMMENT 'Webhook回调地址',
  `webhook_secret` varchar(64) DEFAULT NULL COMMENT 'Webhook签名密钥',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_user` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开放平台应用表';
