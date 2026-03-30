# Gate-5 事件映射联调证据

执行时间：2026-03-30  
环境：本地 Spring Boot + RabbitMQ (`localhost:5672 admin/123456`)  
开关：`event.bridge.mq-enabled=true`，`event.bridge.webhook-enabled=true`

## 触发接口

1. `POST /api/v1/candidate/resume/screen`
2. `POST /api/v1/interview/assistant/answer/evaluate`
3. `POST /api/v1/export/word`

## 核验接口

- `GET /api/v1/internal/evidence/events`

## 核验结果（映射一致）

- `candidate.resume.screen` -> `candidate.resume.screened`
- `interview.assistant.answer.evaluate` -> `interview.answer.evaluated`
- `export.generate` -> `export.generated`

## 结论

- MQ/Webhook 事件映射联调已通过（最小实现）。
