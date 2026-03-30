-- Contract baseline verification SQL for issue #3 (Slice-01)
-- Scope: RFC-1~RFC-4 field and dictionary checks only.

USE `interview_platform`;

-- A) key field existence checks
SELECT 'ai_resume_screen_result.screen_status' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'ai_resume_screen_result'
  AND COLUMN_NAME = 'screen_status';

SELECT 'ai_question_generate_record.interview_id' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'ai_question_generate_record'
  AND COLUMN_NAME = 'interview_id';

SELECT 'ai_question_generate_record.resume_section_id' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'ai_question_generate_record'
  AND COLUMN_NAME = 'resume_section_id';

SELECT 'ai_question_generate_record.input_snapshot_hash' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'ai_question_generate_record'
  AND COLUMN_NAME = 'input_snapshot_hash';

SELECT 'interview_export_task.export_type(0/1/2)' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'interview_export_task'
  AND COLUMN_NAME = 'export_type'
  AND COLUMN_TYPE LIKE 'tinyint%';

SELECT 'interview_export_task.job_code' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'interview_export_task'
  AND COLUMN_NAME = 'job_code';

SELECT 'interview_export_task.file_hash' AS check_item,
       COUNT(*) AS matched_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'interview_platform'
  AND TABLE_NAME = 'interview_export_task'
  AND COLUMN_NAME = 'file_hash';

-- B) dictionary existence checks
SELECT dict_type, COUNT(*) AS item_count
FROM sys_dict
WHERE dict_type IN ('screen_status', 'export_type', 'review_status', 'recommend_level')
  AND is_deleted = 0
GROUP BY dict_type
ORDER BY dict_type;

-- C) export_type value contract checks
SELECT dict_code, dict_name
FROM sys_dict
WHERE dict_type = 'export_type'
  AND is_deleted = 0
ORDER BY CAST(dict_code AS UNSIGNED);
