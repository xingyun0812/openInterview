# 门禁证据包说明（4/5/6）

## 4) 契约回归 100% 证据

- 命令：`mvn test`
- 证据文件：
  - `target/surefire-reports/*.xml`
  - 控制台输出（包含 `Tests run`, `Failures: 0`）
- 判定：回归测试失败数为 0

## 5) MQ/Webhook 事件映射联调证据

### MQ
- 开启：`event.bridge.mq-enabled: true`
- RabbitMQ：`localhost:5672 admin/123456`
- 证据：
  - 接口返回里的 `mqEventCode`
  - `GET /api/v1/internal/evidence/events` 中 `mqEvents[]`

### Webhook
- 开启：`event.bridge.webhook-enabled: true`
- URL：`http://localhost:8080/webhook/mock/receive`（或你的真实地址）
- 证据：
  - 接口返回里的 `webhookEventCode`
  - `GET /api/v1/internal/evidence/events` 中 `webhookEvents[]`

## 6) traceId / bizCode / errorCode 抽检证据

- 成功链路抽检：
  - 任一写接口响应中存在 `traceId` 和 `bizCode`
- 失败链路抽检：
  - 非法请求响应中存在 `traceId`、`bizCode`、`errorCode`
- 建议抽检接口：
  - `POST /api/v1/export/word`（成功）
  - `GET /api/v1/export/task/999999991`（失败）

## 证据归档建议目录

```text
backend/evidence/
  gate4-regression/
  gate5-event-mapping/
  gate6-trace-audit/
```
