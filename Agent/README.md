# 月汐 Lunatide · 开发指引总入口（写给 Codex）

> 你（Codex）是本项目的主力开发者。本目录 `Agent/` 是项目的**需求与规范来源**。
> 开始写任何代码之前，按下面的顺序**完整读完**所有文档。

## 一句话产品定义

**月汐（Lunatide）**：一款安卓经期记录 App。**免费、无需注册、无广告、无社区、不联网**，所有数据只保存在这台手机上（加密存储），用户可以自己导出加密备份。

## 阅读顺序

| 顺序 | 文件 | 内容 | 重要程度 |
|---|---|---|---|
| 1 | `01_PRODUCT_BRIEF.md` | 产品背景、目标用户、做什么/不做什么 | 必读 |
| 2 | `02_PRIVACY_RED_LINES.md` | **隐私红线（不可违反的硬约束）+ 自动化检查** | **最高优先级** |
| 3 | `03_TECH_ARCHITECTURE.md` | 技术栈、依赖白名单、工程结构、分层 | 必读 |
| 4 | `04_FEATURE_SPEC.md` | 每个页面/功能的详细需求与交互规则 | 必读 |
| 5 | `05_DATA_MODEL.md` | 数据库表结构、业务规则、迁移策略 | 必读 |
| 6 | `06_PREDICTION_ENGINE.md` | 周期预测算法 + 必须覆盖的单元测试用例 | 必读 |
| 7 | `07_UI_UX_GUIDE.md` | 视觉风格、配色、文案语气、无障碍 | 必读 |
| 8 | `08_SECURITY_AND_BACKUP.md` | 数据库加密、应用锁、加密备份文件格式 | 必读 |
| 9 | `09_DEV_WORKFLOW_AND_MILESTONES.md` | **开发流程、里程碑、Git 规范、命令** | 必读 |
| 10 | `10_ACCEPTANCE_CHECKLIST.md` | 验收清单（每个里程碑和最终发布都要对照） | 必读 |
| 11 | `11_OPEN_SOURCE_AND_RELEASE.md` | 开源（MIT）、签名、发布脚本、GitHub Release、官网素材包 | 必读 |
| — | `PROGRESS.md` | 进度日志，**由你维护** | 持续更新 |
| — | `DECISIONS.md` | 技术决策记录，**由你维护** | 持续更新 |

## 你必须遵守的总规则（摘要，细节见各文档）

1. **隐私红线高于一切。** 与任何其他需求冲突时，以 `02_PRIVACY_RED_LINES.md` 为准。App 的 Manifest 中**永远不允许出现 `android.permission.INTERNET`**。
2. **按里程碑推进**（见 `09_DEV_WORKFLOW_AND_MILESTONES.md`）。一个里程碑没有通过构建、测试和隐私检查，不进入下一个。
3. **不擅自加依赖。** 只用 `03_TECH_ARCHITECTURE.md` 中的依赖白名单。确实需要新库时，先在 `DECISIONS.md` 写明理由，并确认它不联网、不收集数据。
4. **遇到模糊需求时**：选择「更保护隐私、更简单」的方案，写进 `DECISIONS.md` 后继续开发，不要停下来等。只有不可逆的操作（例如删除远程仓库内容、强推覆盖历史）才停下来问人。
5. **不要修改 `Agent/` 下的规范文档**（`PROGRESS.md`、`DECISIONS.md` 除外）。觉得规范有问题，就在 `DECISIONS.md` 里写「建议修改规范：……」，然后按你的建议实现并说明理由。
6. **每个里程碑结束时**：更新 `PROGRESS.md`，提交 Git，并给人类一段简短汇报（做了什么、怎么验证的、遗留问题）。
7. **诚实。** 没跑过的测试不要说通过；没实现的功能不要标完成；构建环境有问题（例如下载不了 Gradle 依赖）就如实说明。

## 项目基本信息

| 项 | 值 |
|---|---|
| 中文名 | 月汐 |
| 英文名 / 项目代号 | Lunatide |
| applicationId / 包名 | `com.xichugeek.lunatide`（首次发布前可改，发布后不可改） |
| 本地路径 | `D:\Project\Android\Lunatide` |
| GitHub | https://github.com/xichugeek/Lunatide.git |
| 许可证 | MIT，`Copyright (c) 2026 西楚极客 XichuGeek`（开源，公开仓库） |
| 发布渠道 | GitHub Releases + 官网项目页（计划 `https://xichugeek.com/projects/lunatide/`），暂不上架应用商店 |
| 主语言 | 简体中文（同时准备英文资源） |
| 平台 | Android，minSdk 26（Android 8.0），targetSdk/compileSdk 使用当前最新稳定 API |
| 标语 | 不注册、不联网、无广告，只属于你的周期记录 |
