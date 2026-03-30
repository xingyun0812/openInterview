# Issue Executor Playbook (v1)

适用对象：执行类 Agent（按 issue 开发）

## 输入

1. Issue 编号（如 `#4`）
2. 关联 PRD 父 issue（如 `#2`）
3. 当前 wave（如 `wave:2`）

## 执行步骤

1. 读取 issue 描述，确认范围与 blocked-by。
2. 仅实现该 issue 范围，不跨 issue 改造。
3. 完成后必须：
   - 代码变更
   - 测试结果
   - 证据摘要（trace/biz/error、事件映射、回归）
4. 创建 PR，标题包含 issue 号。
5. 在 issue 评论区回贴：
   - PR 链接
   - 测试摘要
   - 风险清单（若无写“无阻塞”）

## 禁止项

1. 不得修改公共契约而不提 RFC。
2. 不得提交构建产物（`backend/target/**`）。
3. 不得处理 `.obsidian/**` 文件。

## 完成定义（DoD）

1. CI 通过（PR Gate）
2. issue 评论已贴证据链接
3. blocked-by 全满足后才可进入下一 wave
