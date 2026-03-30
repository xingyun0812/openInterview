-- V20260327_03__ai_screen_weight_config.sql
-- Screening weight config table

USE `interview_platform`;

CREATE TABLE IF NOT EXISTS `ai_screen_weight_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_code` varchar(32) NOT NULL COMMENT '配置编码',
  `position_type` varchar(32) NOT NULL COMMENT '岗位类型',
  `skill_weight` decimal(5,2) NOT NULL COMMENT '技能匹配权重',
  `project_weight` decimal(5,2) NOT NULL COMMENT '项目相关性权重',
  `experience_weight` decimal(5,2) NOT NULL COMMENT '年限级别权重',
  `stability_weight` decimal(5,2) NOT NULL COMMENT '稳定性权重',
  `pass_line` decimal(5,2) NOT NULL DEFAULT '70.00' COMMENT '推荐阈值',
  `pending_line` decimal(5,2) NOT NULL DEFAULT '60.00' COMMENT '待定阈值',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_user` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_code` (`config_code`),
  KEY `idx_position_type` (`position_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI筛选权重配置表';
