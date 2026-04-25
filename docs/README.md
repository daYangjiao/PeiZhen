# Docs Index

更新时间: 2026-04-25

本目录是当前仓库可提交、可推送的项目说明入口。真实密码、服务器连接、数据库连接等本地机密只保存在根目录 `SECRET_VAULT.md`，该文件已由 `.gitignore` 忽略，不能提交或推送。

## 当前文档

| 文件 | 用途 | 维护时机 |
| --- | --- | --- |
| `BACKEND_MANIFEST.md` | 后端控制器、接口路径、权限和关键响应约定。 | 新增/删除/修改接口、鉴权规则、上传响应或管理端权限时更新。 |
| `FRONTEND_MANIFEST.md` | uni-app 小程序页面、管理端路由、核心交互和主要接口。 | 新增/删除/修改页面、路由、核心跳转、核心交互或前端接口调用时更新。 |
| `DB_MAP.md` | 数据库基线、迁移脚本、核心表结构和关键关系。 | 新增/修改数据库脚本、表、字段、索引或重要数据语义时更新。 |

## 使用规则

- 开始涉及代码、接口、页面或数据库的任务时，先读取本目录三份清单，避免基于过期理解改动。
- 修改后端接口时，同步检查 `BACKEND_MANIFEST.md` 和受影响的前端页面说明。
- 修改小程序页面、管理端路由或核心业务交互时，同步检查 `FRONTEND_MANIFEST.md`。
- 修改数据库或迁移脚本时，同步检查 `DB_MAP.md`。
- 文档只记录可公开的结构和流程，不记录真实密码、Token、私钥、服务器登录信息。

## 当前状态

- 后端接口清单已对齐当前 `src/main/java/org/example/controller/**` 的 19 个控制器和 97 个方法级 HTTP 映射。
- 前端页面清单已对齐当前 `frontend/mini-program/pages.json` 和 `frontend/admin/src/router/index.js`。
- 数据库清单已覆盖 `database/student.sql` 基线、历史 `database/*.sql` 增量和当前 `db/*.sql` 自动部署增量。
