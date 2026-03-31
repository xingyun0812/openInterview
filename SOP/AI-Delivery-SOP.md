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

> AI Review 允许“未配置时跳过”，避免在个人仓库阶段卡住合并；有团队后再把 AI Review 设为 Required。

- **`ANTHROPIC_API_KEY`**（可选）：Claude 直连 API（不配则 `claude-review` 自动跳过）
- **`CLAUDE_CODE_OAUTH_TOKEN`**（可选）：Claude Code OAuth Token（不配则 `claude-review` 自动跳过）
- **`DEEPSEEK_API_KEY`**（可选）：DeepSeek API Key（不配则 `ai-review-deepseek` 自动跳过）
- **`DEEPSEEK_BASE_URL`**（可选）：默认 `https://api.deepseek.com`（若你用私有网关/代理再填）

### 2.2 开启 Auto-merge

`Settings -> General -> Pull Requests -> Allow auto-merge`

### 2.3 Branch protection（建议强制）

对 `main` 开保护：

- Require a pull request before merging
- Require status checks to pass before merging
  - 必选：`backend-mvn-test`
- 个人仓库阶段建议：**Approvals = 0**（避免“作者不能 approve 自己 PR”导致无法合并）
- 团队化后建议：对 HITL（`mode:HITL`）把 approvals 调回 >= 1，并按需把 `claude-code-review` 设为 Required

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

## 3.1 Level3（仅 GitHub Actions、无云）标签与状态机约定

### 3.1.1 PRD 入口（主控识别）

- **`type:prd`**：PRD Issue（主控入口）
- **`orchestrator:split`**：允许 Orchestrator 拆分子 Issue（幂等：重复打也不重复创建）

### 3.1.2 子 Issue 生命周期（主控分发）

- **`status:ready`**：可开工（触发 module agent workflow）
- **`status:in_progress`**：执行中（由 workflow 自动打）
- **`status:blocked`**：阻塞（由 reconcile/执行 workflow 自动打，并评论原因）
- **`status:done`**：完成（由 workflow 自动打，可选自动 close）

### 3.1.3 交付物 DoD（Definition of Done）

每个子 Issue（`type:tracer-bullet`）完成必须满足：

- **PR**：至少 1 个 PR（标题建议包含 issue 编号）
- **CI**：`backend-mvn-test` 绿
- **Evidence**：提交证据目录（建议）：

```text
backend/evidence/issue{N}-{slug}/summary.md
```

`summary.md` 至少包含：Summary / Test plan / Evidence（actions run 或 artifact 链接）/ Risks

---

## 4. PR 规范（必须）

PR 描述必须包含：

- Summary（做了什么）
- Test plan（怎么验证）
- Evidence（证据包链接或 Actions artifact）

