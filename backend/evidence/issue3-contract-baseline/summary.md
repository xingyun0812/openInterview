# Issue #3 Contract Baseline Dry-Run Summary

执行时间：2026-03-30  
执行范围：`[AFK] Slice-01 Contract & Migration Baseline`（RFC-1~RFC-4）  
执行原则：仅验证契约基线，不引入任何新业务功能。

## 1) Dry-run 执行命令

```bash
# 临时容器（隔离环境）
export ISSUE3_MYSQL_ROOT_PASSWORD='<replace-with-local-secret>'
docker run -d --name openinterview_issue3_mysql -e MYSQL_ROOT_PASSWORD="$ISSUE3_MYSQL_ROOT_PASSWORD" mysql:8.0.39

# migration
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/migration/V20260327_01__init_core_tables.sql
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/migration/V20260327_02__create_ai_tables.sql
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/migration/V20260327_03__ai_screen_weight_config.sql
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/migration/V20260327_04__init_enum_dict.sql

# 契约核验
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot -t < db/verification/V20260330_01__verify_contract_baseline.sql

# rollback（逆序）
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/rollback/R20260327_04__rollback_enum_dict.sql
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/rollback/R20260327_03__rollback_ai_screen_weight.sql
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/rollback/R20260327_02__rollback_ai_tables.sql
docker exec -e MYSQL_PWD="$ISSUE3_MYSQL_ROOT_PASSWORD" -i openinterview_issue3_mysql mysql -uroot < db/rollback/R20260327_01__rollback_core_tables.sql
```

## 2) 执行结果摘要

- migration dry-run：通过（4/4）
- rollback dry-run：通过（4/4）
- 关键字段核验：通过
  - `ai_resume_screen_result.screen_status` 存在
  - `ai_question_generate_record.interview_id / resume_section_id / input_snapshot_hash` 存在
  - `interview_export_task.export_type / job_code / file_hash` 存在
- 字典核验：通过
  - `screen_status`: 4 项（1/2/3/4）
  - `export_type`: 3 项（0/1/2）
  - `review_status`: 3 项（1/2/3）
  - `recommend_level`: 3 项（1/2/3）
- 回滚后抽检：关键表已清理（符合 dry-run 预期）

## 2.1) 安全说明（口令处理）

- 该文档中的执行命令已移除明文口令，不再使用 `-p<password>` 形式。
- 当前推荐方式：通过 `ISSUE3_MYSQL_ROOT_PASSWORD` 与 `MYSQL_PWD` 传递本地临时口令，避免把凭据写入仓库。
- 证据日志中的 MySQL warning 来源于早期 dry-run 命令形态（命令行带口令），不影响本次契约核验结论。
- 后续复跑时请继续使用环境变量方案，并将口令保留在本机会话中，不落盘到版本库文件。

## 3) 明细证据文件

- migration 日志：
  - `backend/evidence/issue3-contract-baseline/migration_01.log`
  - `backend/evidence/issue3-contract-baseline/migration_02.log`
  - `backend/evidence/issue3-contract-baseline/migration_03.log`
  - `backend/evidence/issue3-contract-baseline/migration_04.log`
- rollback 日志：
  - `backend/evidence/issue3-contract-baseline/rollback_04.log`
  - `backend/evidence/issue3-contract-baseline/rollback_03.log`
  - `backend/evidence/issue3-contract-baseline/rollback_02.log`
  - `backend/evidence/issue3-contract-baseline/rollback_01.log`
- 契约核验输出：
  - `backend/evidence/issue3-contract-baseline/verification.log`
- 回滚后抽检输出：
  - `backend/evidence/issue3-contract-baseline/post_rollback_check.log`
