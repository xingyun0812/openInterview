# Issue #4 Resume Upload + Parse Async Loop

执行时间：2026-03-30  
执行范围：`[AFK] Slice-02 Resume Upload + Parse Async Loop`

## 覆盖范围

- `POST /api/v1/candidate/resume/upload`
- `POST /api/v1/candidate/resume/parse`
- `GET /api/v1/candidate/resume/parse/result/{candidateId}`
- 异步重试退避：`200ms, 500ms, 1000ms`
- 失败审计：`traceId + bizCode + errorCode + retryCount + exhausted`

## 验证命令

```bash
cd backend
mvn clean test
```

## 结果摘要

- 总测试：43
- 通过：43
- 失败：0
- 核心专项：`Issue4ResumeParseLoopTest`（10/10）

## 最小证据用例

1. 成功链路：上传 + 触发解析 + 轮询结果 `parseStatus=2`
2. 参数缺失：`parse` 缺少 `candidateId` 返回 `1001`
3. 幂等重复：相同 `X-Idempotency-Key` 重复触发 `parse`，`taskCode` 一致
4. 解析失败重试：`fail-always` 文件触发 3 次重试后失败，`parseFailureAudits` 含 `errorCode=8001`

