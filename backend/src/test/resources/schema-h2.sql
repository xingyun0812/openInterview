-- H2 compatible schema (from V20260327_01 + V20260327_02)

CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  dict_type VARCHAR(64) NOT NULL,
  dict_code VARCHAR(64) NOT NULL,
  dict_name VARCHAR(128) NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_dict_type_code UNIQUE (dict_type, dict_code)
);

CREATE INDEX IF NOT EXISTS idx_dict_type ON sys_dict (dict_type);

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(32) NOT NULL,
  phone VARCHAR(16),
  email VARCHAR(64),
  dept_id BIGINT,
  role_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_time TIMESTAMP,
  last_login_ip VARCHAR(32),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_role_id ON sys_user (role_id);
CREATE INDEX IF NOT EXISTS idx_dept_id ON sys_user (dept_id);

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(32) NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  description VARCHAR(256),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_role_code UNIQUE (role_code)
);

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_name VARCHAR(32) NOT NULL,
  menu_code VARCHAR(64) NOT NULL,
  menu_type TINYINT NOT NULL,
  path VARCHAR(128),
  icon VARCHAR(64),
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_menu_code UNIQUE (menu_code)
);

CREATE INDEX IF NOT EXISTS idx_parent_id ON sys_menu (parent_id);

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

CREATE INDEX IF NOT EXISTS idx_menu_id ON sys_role_menu (menu_id);

CREATE TABLE IF NOT EXISTS interview_candidate (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  candidate_code VARCHAR(32) NOT NULL,
  name VARCHAR(32) NOT NULL,
  phone VARCHAR(16) NOT NULL,
  email VARCHAR(64),
  id_card VARCHAR(32),
  gender TINYINT,
  age INT,
  apply_position VARCHAR(64) NOT NULL,
  work_years VARCHAR(16),
  highest_education VARCHAR(16),
  resume_url VARCHAR(256),
  source VARCHAR(32),
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_candidate_code UNIQUE (candidate_code)
);

CREATE INDEX IF NOT EXISTS idx_phone ON interview_candidate (phone);
CREATE INDEX IF NOT EXISTS idx_apply_position ON interview_candidate (apply_position);
CREATE INDEX IF NOT EXISTS idx_status ON interview_candidate (status);

CREATE TABLE IF NOT EXISTS interview_score_template (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  template_name VARCHAR(64) NOT NULL,
  template_code VARCHAR(32) NOT NULL,
  apply_position_type VARCHAR(32),
  interview_round VARCHAR(16),
  full_score INT NOT NULL DEFAULT 100,
  pass_score INT NOT NULL DEFAULT 60,
  score_rule TINYINT NOT NULL DEFAULT 1,
  description VARCHAR(256),
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_template_code UNIQUE (template_code)
);

CREATE TABLE IF NOT EXISTS interview_score_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  item_name VARCHAR(64) NOT NULL,
  item_desc VARCHAR(256),
  full_score INT NOT NULL,
  weight DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  sort INT NOT NULL DEFAULT 0,
  is_required TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_template_id ON interview_score_item (template_id);

CREATE TABLE IF NOT EXISTS interview_plan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  interview_code VARCHAR(32) NOT NULL,
  candidate_id BIGINT NOT NULL,
  apply_position VARCHAR(64) NOT NULL,
  interview_round VARCHAR(16) NOT NULL,
  interview_type TINYINT NOT NULL,
  template_id BIGINT NOT NULL,
  interview_start_time TIMESTAMP NOT NULL,
  interview_end_time TIMESTAMP NOT NULL,
  interview_room_id VARCHAR(64) NOT NULL,
  interview_room_link VARCHAR(256) NOT NULL,
  hr_user_id BIGINT NOT NULL,
  interviewer_ids VARCHAR(512) NOT NULL,
  interview_status TINYINT NOT NULL DEFAULT 1,
  interview_result TINYINT,
  final_score DECIMAL(5,2),
  is_signed TINYINT NOT NULL DEFAULT 0,
  record_file_url VARCHAR(256),
  remark VARCHAR(512),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_interview_code UNIQUE (interview_code)
);

CREATE INDEX IF NOT EXISTS idx_candidate_id ON interview_plan (candidate_id);
CREATE INDEX IF NOT EXISTS idx_hr_user_id ON interview_plan (hr_user_id);
CREATE INDEX IF NOT EXISTS idx_interview_status ON interview_plan (interview_status);
CREATE INDEX IF NOT EXISTS idx_interview_time ON interview_plan (interview_start_time, interview_end_time);

CREATE TABLE IF NOT EXISTS interview_evaluate (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  interview_id BIGINT NOT NULL,
  interviewer_id BIGINT NOT NULL,
  interviewer_name VARCHAR(32) NOT NULL,
  total_score DECIMAL(5,2) NOT NULL,
  interview_result TINYINT NOT NULL,
  advantage_comment CLOB,
  disadvantage_comment CLOB,
  comprehensive_comment CLOB NOT NULL,
  submit_status TINYINT NOT NULL DEFAULT 0,
  submit_time TIMESTAMP,
  is_signed TINYINT NOT NULL DEFAULT 0,
  sign_time TIMESTAMP,
  sign_img_url VARCHAR(256),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_interview_interviewer UNIQUE (interview_id, interviewer_id)
);

CREATE INDEX IF NOT EXISTS idx_interview_id ON interview_evaluate (interview_id);
CREATE INDEX IF NOT EXISTS idx_interviewer_id ON interview_evaluate (interviewer_id);
CREATE INDEX IF NOT EXISTS idx_submit_status ON interview_evaluate (submit_status);

CREATE TABLE IF NOT EXISTS interview_score_detail (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  evaluate_id BIGINT NOT NULL,
  interview_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  item_name VARCHAR(64) NOT NULL,
  item_full_score INT NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  item_comment VARCHAR(256),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_evaluate_item UNIQUE (evaluate_id, item_id)
);

CREATE INDEX IF NOT EXISTS idx_evaluate_id ON interview_score_detail (evaluate_id);
CREATE INDEX IF NOT EXISTS idx_interview_id_sd ON interview_score_detail (interview_id);

CREATE TABLE IF NOT EXISTS interview_signature (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sign_code VARCHAR(32) NOT NULL,
  interview_id BIGINT NOT NULL,
  evaluate_id BIGINT,
  sign_user_id BIGINT NOT NULL,
  sign_user_name VARCHAR(32) NOT NULL,
  sign_type TINYINT NOT NULL,
  sign_img_url VARCHAR(256) NOT NULL,
  sign_time TIMESTAMP NOT NULL,
  sign_ip VARCHAR(32) NOT NULL,
  sign_device VARCHAR(128),
  file_hash VARCHAR(64) NOT NULL,
  certificate_no VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_sign_code UNIQUE (sign_code)
);

CREATE INDEX IF NOT EXISTS idx_interview_id_sig ON interview_signature (interview_id);
CREATE INDEX IF NOT EXISTS idx_evaluate_id_sig ON interview_signature (evaluate_id);
CREATE INDEX IF NOT EXISTS idx_sign_user_id ON interview_signature (sign_user_id);

CREATE TABLE IF NOT EXISTS interview_export_task (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_code VARCHAR(32) NOT NULL,
  export_type TINYINT NOT NULL,
  export_content VARCHAR(512) NOT NULL,
  job_code VARCHAR(64),
  template_id BIGINT,
  export_user_id BIGINT NOT NULL,
  export_user_name VARCHAR(32) NOT NULL,
  task_status TINYINT NOT NULL DEFAULT 1,
  file_url VARCHAR(256),
  file_name VARCHAR(128),
  file_size BIGINT,
  file_hash VARCHAR(64),
  fail_reason VARCHAR(256),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_task_code UNIQUE (task_code)
);

CREATE INDEX IF NOT EXISTS idx_export_user_id ON interview_export_task (export_user_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON interview_export_task (task_status);

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  log_code VARCHAR(32) NOT NULL,
  trace_id VARCHAR(64),
  biz_code VARCHAR(64),
  error_code VARCHAR(32),
  user_id BIGINT,
  user_name VARCHAR(32),
  operation_module VARCHAR(32) NOT NULL,
  operation_type VARCHAR(32) NOT NULL,
  operation_desc VARCHAR(256) NOT NULL,
  request_url VARCHAR(256),
  request_method VARCHAR(16),
  request_params CLOB,
  response_result CLOB,
  ip_address VARCHAR(32),
  device_info VARCHAR(256),
  operation_time TIMESTAMP NOT NULL,
  cost_time INT NOT NULL,
  operation_status TINYINT NOT NULL,
  fail_reason VARCHAR(256),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_id_log ON sys_operation_log (user_id);
CREATE INDEX IF NOT EXISTS idx_operation_time ON sys_operation_log (operation_time);
CREATE INDEX IF NOT EXISTS idx_operation_module ON sys_operation_log (operation_module);
CREATE INDEX IF NOT EXISTS idx_trace_id ON sys_operation_log (trace_id);

CREATE TABLE IF NOT EXISTS open_api_app (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  app_id VARCHAR(32) NOT NULL,
  app_secret VARCHAR(64) NOT NULL,
  app_name VARCHAR(64) NOT NULL,
  app_desc VARCHAR(256),
  api_permissions VARCHAR(1024),
  webhook_url VARCHAR(256),
  webhook_secret VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  create_user BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_app_id UNIQUE (app_id)
);

CREATE TABLE IF NOT EXISTS ai_resume_parse_result (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  candidate_id BIGINT NOT NULL,
  resume_file_url VARCHAR(256) NOT NULL,
  parse_status TINYINT NOT NULL DEFAULT 1,
  basic_info_json CLOB,
  education_json CLOB,
  work_experience_json CLOB,
  project_json CLOB,
  skill_tags_json CLOB,
  raw_text CLOB,
  fail_reason VARCHAR(256),
  model_name VARCHAR(64),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_candidate_id_parse ON ai_resume_parse_result (candidate_id);
CREATE INDEX IF NOT EXISTS idx_parse_status ON ai_resume_parse_result (parse_status);

CREATE TABLE IF NOT EXISTS ai_resume_screen_result (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  candidate_id BIGINT NOT NULL,
  job_code VARCHAR(64) NOT NULL,
  screen_status TINYINT NOT NULL DEFAULT 1,
  match_score DECIMAL(5,2),
  recommend_level TINYINT,
  review_result TINYINT,
  review_user_id BIGINT,
  review_time TIMESTAMP,
  review_comment VARCHAR(256),
  core_reason VARCHAR(512),
  missing_skills VARCHAR(512),
  risk_flags VARCHAR(512),
  input_snapshot_hash VARCHAR(64) NOT NULL,
  model_name VARCHAR(64),
  fail_reason VARCHAR(256),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_candidate_id_screen ON ai_resume_screen_result (candidate_id);
CREATE INDEX IF NOT EXISTS idx_job_code ON ai_resume_screen_result (job_code);
CREATE INDEX IF NOT EXISTS idx_screen_status ON ai_resume_screen_result (screen_status);

CREATE TABLE IF NOT EXISTS ai_question_generate_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  request_code VARCHAR(32) NOT NULL,
  interview_id BIGINT NOT NULL,
  resume_section_id VARCHAR(64) NOT NULL,
  input_snapshot_hash VARCHAR(64) NOT NULL,
  apply_position VARCHAR(64) NOT NULL,
  difficulty_level TINYINT NOT NULL,
  tech_stack VARCHAR(256),
  question_count INT NOT NULL,
  question_payload_json CLOB NOT NULL,
  review_status TINYINT NOT NULL DEFAULT 1,
  review_user_id BIGINT,
  review_comment VARCHAR(256),
  model_name VARCHAR(64),
  create_user BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_request_code UNIQUE (request_code)
);

CREATE INDEX IF NOT EXISTS idx_interview_id_q ON ai_question_generate_record (interview_id);

CREATE TABLE IF NOT EXISTS interview_answer_assess_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  record_code VARCHAR(32) NOT NULL,
  interview_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  answer_text CLOB,
  accuracy_score DECIMAL(5,2),
  coverage_score DECIMAL(5,2),
  clarity_score DECIMAL(5,2),
  total_score DECIMAL(5,2),
  follow_up_suggest VARCHAR(512),
  model_name VARCHAR(64),
  create_user BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_record_code UNIQUE (record_code)
);

CREATE INDEX IF NOT EXISTS idx_interview_id_a ON interview_answer_assess_record (interview_id);
CREATE INDEX IF NOT EXISTS idx_question_id_a ON interview_answer_assess_record (question_id);
CREATE INDEX IF NOT EXISTS idx_candidate_id_a ON interview_answer_assess_record (candidate_id);

-- Default RBAC roles for integration tests (idempotent)
INSERT INTO sys_role (role_name, role_code, description, create_time, update_time, is_deleted)
SELECT 'HR', 'HR', 'HR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'HR');
INSERT INTO sys_role (role_name, role_code, description, create_time, update_time, is_deleted)
SELECT 'Interviewer', 'INTERVIEWER', 'Interviewer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'INTERVIEWER');
INSERT INTO sys_role (role_name, role_code, description, create_time, update_time, is_deleted)
SELECT 'Admin', 'ADMIN', 'Admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'ADMIN');
