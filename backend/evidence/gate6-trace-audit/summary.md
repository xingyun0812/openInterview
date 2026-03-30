# Gate-6 Trace/Biz/Error 抽检证据

执行时间：2026-03-30

## 抽检样本

### 成功链路

- `POST /api/v1/candidate/resume/screen`
- 返回包含：
  - `traceId=trace-live-01`
  - `bizCode=AISCREEN...`
  - `errorCode=null`

### 失败链路

- `GET /api/v1/export/task/999999991`（测试集中已覆盖）
- 返回包含：
  - `traceId`
  - `bizCode`
  - `errorCode`（非空）

## 结论

- `traceId + bizCode + errorCode` 字段在成功/失败路径均可抽检到。
