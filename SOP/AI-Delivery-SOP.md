# AI Delivery SOP（阶段1 + 阶段2）

适用仓库：`openInterview`  
目标：把“Issue -> 开发 -> PR -> 门禁证据 -> Review -> 自动合并”标准化与自动化，尽量降低人工介入。

---

## 0. 术语

- **AFK**：无需人工阻塞决策的切片（自动跑、自动合并）
- **HITL**：需要人工参与（策略/合规/质量审查）的切片
- **证据包**：门禁要求的测试/联调/trace 抽检摘要

---

## 1. 团队分工（最小）

- **Tech Lead（你）**：配置一次性权限与门禁规则；HITL 的人工审批
- **执行者（人或Agent）**：按 issue 领取与提交 PR；必要时补充证据
- **CI（GitHub Actions）**：跑测试、产出证据包、AI Review、自动合并（AFK）

---

## 2. 必须配置（GitHub Settings）

### 2.1 Secrets（Repository -> Settings -> Secrets and variables -> Actions）

> DeepSeek 采用 OpenAI 兼容接口；不要把 key 写入仓库。

- **`DEEPSEEK_API_KEY`**（必填）：DeepSeek API Key
- **`DEEPSEEK_BASE_URL`**（可选）：默认 `https://api.deepseek.com`（若你用私有网关/代理再填）

### 2.2 开启 Auto-merge

`Settings -> General -> Pull Requests -> Allow auto-merge`

### 2.3 Branch protection（建议强制）

对 `main` 开保护：

- Require a pull request before merging
- Require status checks to pass before merging
  - 必选：`ci/backend-mvn-test`
  - 必选：`ai-review/deepseek`
- 对 HITL（`mode:HITL`）建议：Require approvals >= 1

---

## 3. Issue 规范（执行入口）

每个切片 issue 必须具备：

- `type:tracer-bullet`
- `mode:AFK` 或 `mode:HITL`
- `priority:P0/P1/P2`
- `module:*`
- `wave:*`

执行建议：

- Wave-1 先做
- Wave-2/3 并行做
- HITL（如出题审核策略）在 PR 阶段必须人工 review 才能合并

---

## 4. PR 规范（必须）

PR 描述必须包含：

- Summary（做了什么）
- Test plan（怎么验证）
- Evidence（证据包链接或 Actions artifact）

