# Webhook 接入说明（本地联调）

## 1. 开启开关

编辑 `src/main/resources/application.yml`：

- `event.bridge.webhook-enabled: true`
- `event.bridge.webhook-url: <你的回调地址>`

如果只是本机自测，可直接用项目内置 mock：

- `event.bridge.webhook-url: http://localhost:8080/webhook/mock/receive`

## 2. 启动服务

```bash
cd backend
mvn spring-boot:run
```

## 3. 触发事件

示例（筛选）：

```bash
curl -X POST "http://localhost:8080/api/v1/candidate/resume/screen" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: webhook-demo-01" \
  -H "X-Trace-Id: trace-webhook-01" \
  -d '{"candidateId":10001,"jobCode":"JAVA_ADV_01"}'
```

## 4. 查询 webhook 收到的事件

```bash
curl "http://localhost:8080/api/v1/internal/evidence/events"
```

返回中 `data.webhookEvents` 即 webhook 接收记录。

## 5. 事件映射口径（固定）

- `candidate.resume.screen` -> `candidate.resume.screened`
- `candidate.resume.parse` -> `candidate.resume.parsed`
- `interview.assistant.question.generate` -> `interview.question.generated`
- `interview.assistant.answer.evaluate` -> `interview.answer.evaluated`
- `export.generate` -> `export.generated`
