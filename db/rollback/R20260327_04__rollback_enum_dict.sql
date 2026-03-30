-- R20260327_04__rollback_enum_dict.sql
-- non-destructive rollback for dictionary initialization

USE `interview_platform`;

UPDATE `sys_dict`
SET `is_deleted` = 1, `status` = 0
WHERE `dict_type` IN ('recommend_level', 'review_status', 'screen_status', 'export_type');
