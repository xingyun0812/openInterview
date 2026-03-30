# Issue-5 Screening + HR Review Gate 证据

执行时间：2026-03-30  
范围：仅包含 #5（筛选与复核接口、状态机与人审闸门、审计与事件）

## 本次实现点

1. 接口：
   - `POST /api/v1/candidate/resume/screen`
   - `GET /api/v1/candidate/resume/screen/result/{candidateId}`
   - `POST /api/v1/candidate/resume/screen/review`
2. 状态机：
   - `screen_status` 合法流转校验（`1->2/3/4`, `3->1`）
   - `review` 前置条件校验（仅 `screen_status=2` 且筛选结果完整可复核）
3. 人审闸门：
   - 响应增加 `aiSuggestionOnly=true`
   - AI 侧不直接写入 `reviewResult`，仅给建议
4. 审计与事件：
   - 关键动作写入审计记录（触发筛选、人工复核）
   - MQ/Webhook 事件命名遵循映射口径（`candidate.resume.screen` -> `candidate.resume.screened`）

## 自动化测试

- 回归：`ContractBaselineApiTest` + `Regression30ApiTest`
- 新增：`Issue5ScreeningReviewGateTest`
- 覆盖点：
  - 人审闸门字段校验
  - 非法复核结论拦截
  - 非法状态流转拦截
  - 并发幂等（筛选）
  - 审计记录落地
