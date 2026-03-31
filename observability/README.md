# observability（阶段3A）

本目录提供最小模板（Prometheus 告警规则等），你们按实际 metrics 名称与 label 补齐。

## Prometheus 告警

- 文件：`observability/prometheus/alerts.yaml`
- 关注：
  - 5xx 错误率
  - P95 延迟
  - MQ/DLQ 堆积（后续接入 RabbitMQ exporter 再补规则）

