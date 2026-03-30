-- V20260327_04__init_enum_dict.sql
-- Initialize enum dictionaries for contract baseline v1

USE `interview_platform`;

INSERT INTO `sys_dict` (`dict_type`, `dict_code`, `dict_name`, `sort`, `status`, `is_deleted`)
VALUES
('recommend_level', '1', '推荐', 1, 1, 0),
('recommend_level', '2', '待定', 2, 1, 0),
('recommend_level', '3', '不推荐', 3, 1, 0),
('review_status', '1', '待审核', 1, 1, 0),
('review_status', '2', '通过', 2, 1, 0),
('review_status', '3', '驳回', 3, 1, 0),
('screen_status', '1', '处理中', 1, 1, 0),
('screen_status', '2', '成功', 2, 1, 0),
('screen_status', '3', '失败', 3, 1, 0),
('screen_status', '4', '已取消', 4, 1, 0),
('export_type', '0', '筛选Excel', 1, 1, 0),
('export_type', '1', '成绩Excel', 2, 1, 0),
('export_type', '2', '面试Word', 3, 1, 0)
ON DUPLICATE KEY UPDATE
  `dict_name` = VALUES(`dict_name`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `is_deleted` = VALUES(`is_deleted`);
