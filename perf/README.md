# perf（阶段3A）

## k6（本地最小可执行）

前提：后端已启动（默认 `http://localhost:8080`）。

```bash
brew install k6
BASE_URL=http://localhost:8080 k6 run perf/k6/smoke-api.js
```

输出解读：
- P95 目标：< 800ms
- 失败率目标：< 1%

