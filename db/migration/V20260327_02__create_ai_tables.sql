-- V20260327_02__create_ai_tables.sql
-- AI extension schema (RFC-1/RFC-4 aligned)

USE `interview_platform`;

-- 15. AI简历解析结果表
CREATE TABLE IF NOT EXISTS `ai_resume_parse_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `resume_file_url` varchar(256) NOT NULL COMMENT '简历文件地址',
  `parse_status` tinyint NOT NULL DEFAULT '1' COMMENT '解析状态 1-处理中 2-成功 3-失败',
  `basic_info_json` json DEFAULT NULL COMMENT '基础信息JSON',
  `education_json` json DEFAULT NULL COMMENT '教育经历JSON',
  `work_experience_json` json DEFAULT NULL COMMENT '工作经历JSON',
  `project_json` json DEFAULT NULL COMMENT '项目经历JSON',
  `skill_tags_json` json DEFAULT NULL COMMENT '技能标签JSON',
  `raw_text` longtext COMMENT '简历纯文本',
  `fail_reason` varchar(256) DEFAULT NULL COMMENT '失败原因',
  `model_name` varchar(64) DEFAULT NULL COMMENT '模型名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_parse_status` (`parse_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI简历解析结果表';

-- 16. AI简历筛选结果表（RFC-1）
CREATE TABLE IF NOT EXISTS `ai_resume_screen_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `job_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `screen_status` tinyint NOT NULL DEFAULT '1' COMMENT '筛选任务状态 1-处理中 2-成功 3-失败 4-已取消',
  `match_score` decimal(5,2) DEFAULT NULL COMMENT '匹配分（0-100），screen_status=2时必须有值',
  `recommend_level` tinyint DEFAULT NULL COMMENT '推荐等级 1-推荐 2-待定 3-不推荐',
  `review_result` tinyint DEFAULT NULL COMMENT 'HR复核结论 1-通过筛选 2-待定 3-淘汰',
  `review_user_id` bigint DEFAULT NULL COMMENT '复核人ID',
  `review_time` datetime DEFAULT NULL COMMENT '复核时间',
  `review_comment` varchar(256) DEFAULT NULL COMMENT '复核备注',
  `core_reason` varchar(512) DEFAULT NULL COMMENT '核心理由摘要',
  `missing_skills` varchar(512) DEFAULT NULL COMMENT '缺失技能关键词',
  `risk_flags` varchar(512) DEFAULT NULL COMMENT '风险标签',
  `input_snapshot_hash` varchar(64) NOT NULL COMMENT '输入快照哈希',
  `model_name` varchar(64) DEFAULT NULL COMMENT '模型名称',
  `fail_reason` varchar(256) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_job_code` (`job_code`),
  KEY `idx_screen_status` (`screen_status`),
  KEY `idx_recommend_level` (`recommend_level`),
  KEY `idx_review_result` (`review_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI简历筛选结果表';

-- 17. AI题目生成记录表（RFC-4）
CREATE TABLE IF NOT EXISTS `ai_question_generate_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `request_code` varchar(32) NOT NULL COMMENT '生成请求编码',
  `interview_id` bigint NOT NULL COMMENT '面试ID',
  `resume_section_id` varchar(64) NOT NULL COMMENT '简历片段标识',
  `input_snapshot_hash` varchar(64) NOT NULL COMMENT '输入快照哈希',
  `apply_position` varchar(64) NOT NULL COMMENT '岗位',
  `difficulty_level` tinyint NOT NULL COMMENT '难度 1-初级 2-中级 3-高级',
  `tech_stack` varchar(256) DEFAULT NULL COMMENT '技术栈关键词',
  `question_count` int NOT NULL COMMENT '生成题目数量',
  `question_payload_json` json NOT NULL COMMENT '题目内容JSON',
  `review_status` tinyint NOT NULL DEFAULT '1' COMMENT '审核状态 1-待审核 2-通过 3-驳回',
  `review_user_id` bigint DEFAULT NULL COMMENT '审核人',
  `review_comment` varchar(256) DEFAULT NULL COMMENT '审核意见',
  `model_name` varchar(64) DEFAULT NULL COMMENT '模型名称',
  `create_user` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_code` (`request_code`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_apply_position` (`apply_position`),
  KEY `idx_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI题目生成记录表';

-- 18. 面试回答评估记录表
CREATE TABLE IF NOT EXISTS `interview_answer_assess_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_code` varchar(32) NOT NULL COMMENT '评估记录编码',
  `interview_id` bigint NOT NULL COMMENT '面试ID',
  `question_id` bigint NOT NULL COMMENT '问题ID',
  `candidate_id` bigint NOT NULL COMMENT '候选人ID',
  `answer_text` text COMMENT '候选人回答文本',
  `accuracy_score` decimal(5,2) DEFAULT NULL COMMENT '准确性得分',
  `coverage_score` decimal(5,2) DEFAULT NULL COMMENT '覆盖度得分',
  `clarity_score` decimal(5,2) DEFAULT NULL COMMENT '表达清晰度得分',
  `total_score` decimal(5,2) DEFAULT NULL COMMENT '综合得分',
  `follow_up_suggest` varchar(512) DEFAULT NULL COMMENT '建议追问',
  `model_name` varchar(64) DEFAULT NULL COMMENT '模型名称',
  `create_user` bigint NOT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_code` (`record_code`),
  KEY `idx_interview_id` (`interview_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_candidate_id` (`candidate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试回答评估记录表';
