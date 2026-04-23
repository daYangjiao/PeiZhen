# AI Instructions

本文件是项目级永久记忆入口，适用于 Codex、Claude Code 和后续接手本仓库的 AI 助手。

## 会话启动必读

每次新会话开始，在分析或修改代码前，必须先读取：

- `AI_INSTRUCTIONS.md`
- `docs/BACKEND_MANIFEST.md`
- `docs/FRONTEND_MANIFEST.md`
- `docs/DB_MAP.md`

只有在需要部署、连接服务器、连接数据库、排查线上环境或读取真实凭据时，才读取本地 `SECRET_VAULT.md`。

## 机密规则

- `SECRET_VAULT.md` 是本地机密金库，已经被 `.gitignore` 排除，严禁提交或推送。
- 不得把 SSH 密码、数据库密码、API Key、Token、私钥内容写入任何可提交文件。
- 最终回复不得回显 `SECRET_VAULT.md` 中的真实机密；只能说明是否已读取、是否已使用。

## 清单同步规则

- 修改后端控制器路径、HTTP 方法、鉴权规则或接口功能时，必须同步更新 `docs/BACKEND_MANIFEST.md`。
- 修改 uni-app `pages.json`、小程序页面核心交互、管理端 Vue Router、管理端 API 封装或页面核心操作时，必须同步更新 `docs/FRONTEND_MANIFEST.md`。
- 修改数据库结构、数据库初始化数据或迁移脚本时，必须同步更新 `docs/DB_MAP.md`。
- 数据库变更必须新增日期命名脚本：`db/YYYYMMDD_descriptive_name.sql`。

## 项目边界

- 后端是 Spring Boot，主代码位于 `src/main/java/org/example`。
- 小程序端位于 `frontend/mini-program`，页面入口以 `frontend/mini-program/pages.json` 为准。
- 管理端位于 `frontend/admin`，路由入口以 `frontend/admin/src/router/index.js` 为准。
- 现有 `database/` 目录保留为历史/基线脚本；后续新增数据库变更统一放入根目录 `db/`。

## 提交规则

- 文档、清单和 `db/` 规则文件可以正常提交到 Gitee。
- `SECRET_VAULT.md` 永远不能提交。
- 除密码、Token、API Key、私钥内容和 `SECRET_VAULT.md` 这类真实机密外，项目中的代码、文档、脚本、数据库变更、部署配置都要正常提交并推送到 Gitee。
- 每次涉及发布、同步或提交后，必须核对本地工作区、Gitee `origin/FF`、服务器 `/home/ops/PZ_yuanbao` 和服务器 release 记录是否三方一致；如果不一致，必须说明差异并继续修复，不能只报告“已推送”。
- 工作区若已有用户或其他工具产生的业务代码改动，未经明确要求不得回滚、覆盖或混入无关提交。
