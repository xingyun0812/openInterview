-- R20260327_01__rollback_core_tables.sql
-- WARNING: This script is for non-production dry-run validation only.
-- Production rollback should follow "stop writes + feature downgrade" strategy.

USE `interview_platform`;

-- soft rollback of dictionary entries
UPDATE `sys_dict`
SET `is_deleted` = 1, `status` = 0
WHERE `dict_type` IN ('recommend_level', 'review_status', 'screen_status', 'export_type');

-- hard rollback for dry-run environment
DROP TABLE IF EXISTS `open_api_app`;
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `interview_export_task`;
DROP TABLE IF EXISTS `interview_signature`;
DROP TABLE IF EXISTS `interview_score_detail`;
DROP TABLE IF EXISTS `interview_evaluate`;
DROP TABLE IF EXISTS `interview_plan`;
DROP TABLE IF EXISTS `interview_score_item`;
DROP TABLE IF EXISTS `interview_score_template`;
DROP TABLE IF EXISTS `interview_candidate`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_dict`;
