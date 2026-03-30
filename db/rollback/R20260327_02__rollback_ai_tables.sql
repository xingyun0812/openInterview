-- R20260327_02__rollback_ai_tables.sql
-- WARNING: This script is for non-production dry-run validation only.
-- Production rollback should avoid destructive drops.

USE `interview_platform`;

DROP TABLE IF EXISTS `interview_answer_assess_record`;
DROP TABLE IF EXISTS `ai_question_generate_record`;
DROP TABLE IF EXISTS `ai_resume_screen_result`;
DROP TABLE IF EXISTS `ai_resume_parse_result`;
