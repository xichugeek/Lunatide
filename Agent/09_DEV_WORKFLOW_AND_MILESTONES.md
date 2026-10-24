# 09 · 开发流程与里程碑

## 1. 开工前（只做一次）

1. 完整阅读 `Agent/` 下全部文档。
2. 检查环境，并把结果写入 `PROGRESS.md` 的「环境」小节：
   - 操作系统（本项目在 Windows 上：`D:\Project\Android\Lunatide`）
   - `java -version`（需要 JDK 17+；可使用 Android Studio 自带 JBR）
   - `ANDROID_HOME` / `ANDROID_SDK_ROOT` 或 `local.properties` 中的 `sdk.dir`
   - 是否有可用的模拟器/真机（`adb devices`）
   - 能否访问 Google Maven / Maven Central（Gradle 依赖下载）
3. 若缺少 JDK、Android SDK 或无法下载依赖：**如实报告并停止**，告诉人类需要做什么（例如「请在 Android Studio 中安装 SDK Platform XX」）。**不要**手工下载 jar 放进仓库，**不要**降级到过时的工具链来绕过问题。

## 2. Git 规范

### 2.1 初始化（M0）
本地目录目前不是 Git 仓库（只有 `Agent/` 目录）；远程仓库已在 GitHub 创建。
```bash
git init -b main
git remote add origin https://github.com/xichugeek/Lunatide.git
git fetch origin
# 若远程 main 已有提交（例如 GitHub 自动生成的 README/LICENSE）：
git pull origin main --allow-unrelated-histories   # 有冲突时保留双方内容并合理合并
```
- **永远不要** `git push --force` 到 `main`。

### 2.2 提交
- 使用 Conventional Commits，描述用中文或英文均可，例如：
  - `feat(today): 首页状态卡片与例假开始/结束按钮`
  - `fix(prediction): 推迟时预测经期未顺延`
  - `test(backup): 增加篡改头部的解密失败用例`
  - `chore(build): 添加 verifyPrivacy 任务`
- 一个里程碑内可以有多次提交；每次提交都应能编译通过。
- 每个里程碑完成并通过验证后：`git push origin main`。若推送因认证失败，保留本地提交，在汇报中说明，**继续开发下一个里程碑**。

### 2.3 绝不提交
`local.properties`、`keystore.properties`、`*.jks`、`*.keystore`、`build/`、`.gradle/`、`.idea/`（可保留 `.idea/codeStyles` 等共享配置，自行判断）、`*.apk`、`*.aab`、任何真实用户数据或备份文件。

## 3. 常用命令（Windows PowerShell）

```powershell
.\gradlew.bat verifyPrivacy          # 隐私检查（权限 / 依赖 / 源码禁用 API）
.\gradlew.bat testDebugUnitTest      # JVM 单元测试
.\gradlew.bat lintDebug              # Android Lint
.\gradlew.bat assembleDebug          # 构建 debug APK
.\gradlew.bat connectedDebugAndroidTest   # 仪器测试（需要模拟器/真机）
.\gradlew.bat assembleRelease        # 构建 release APK（M10）
```
（在 Linux/macOS/CI 中把 `.\gradlew.bat` 换成 `./gradlew`。）

**每个里程碑的「完成标准」至少包括**：`verifyPrivacy`、`testDebugUnitTest`、`lintDebug`（无新增 error）、`assembleDebug` 全部成功。

## 4. 里程碑

每个里程碑：实现 → 写测试 → 运行完成标准中的命令 → 对照 `10_ACCEPTANCE_CHECKLIST.md` 自查 → 更新 `PROGRESS.md` → 提交并推送 → 向人类简短汇报。

### M0 · 工程初始化与隐私防线
- Git 初始化（§2.1）；`.gitignore`。
- Gradle 工程（Kotlin DSL + Version Catalog + Gradle Wrapper），单 `app` 模块，包名 `com.xichugeek.lunatide`。
- Compose + Material 3 + Hilt 跑通一个空白「今天」页面；底部导航三个 Tab 占位。
- Manifest 加固：`allowBackup=false`、`fullBackupContent=false`、`dataExtractionRules`、网络权限 `tools:node="remove"`。
- **`verifyPrivacy` 任务**（`02_PRIVACY_RED_LINES.md` §3 全部三项）并挂到 `check`。
- 写一个“故意失败”的验证：临时加 INTERNET 权限确认任务会失败，然后删除（在 `PROGRESS.md` 记录验证过）。
- `.github/workflows/android.yml`。
- 根目录 `AGENTS.md`（简短：项目一句话、必读 `Agent/README.md`、隐私红线摘要、常用命令、提交前自查清单）。
- 根目录 `README.md`（中文，面向用户：月汐是什么、隐私承诺、如何构建）和 `PRIVACY.md` 初稿。
- 开源基础文件（见 `11_OPEN_SOURCE_AND_RELEASE.md` §1）：`LICENSE`（MIT）、`SECURITY.md` 初稿、`CHANGELOG.md`、`.github/ISSUE_TEMPLATE/` 两个模板。
- `versionCode` 由 `versionName` 自动计算（`11_OPEN_SOURCE_AND_RELEASE.md` §3.1）。
- `PROGRESS.md` 记录环境信息。

### M1 · 数据层
- Room 数据库（`periods`、`day_logs`、`day_symptoms`）、DAO、TypeConverter、schema 导出。
- SQLCipher 集成 + Keystore 密钥管理（`08_SECURITY_AND_BACKUP.md` §1）。
- `SettingsRepository`（DataStore）。
- `Clock`/`TodayProvider` 抽象。
- `CycleRepository`、`DayLogRepository`，领域模型与 mapper。
- 经期编辑用例（`05_DATA_MODEL.md` §3）+ 完整单元测试。
- 仪器测试：SQLCipher 往返（无设备时写好并标注）。

### M2 · 预测引擎与统计
- `domain/prediction`、`domain/stats` 按 `06_PREDICTION_ENGINE.md` 实现。
- §8 全部 25 个测试用例 + 你认为必要的补充用例。

### M3 · 首次引导 + 今天页
- 引导 5 页（`04_FEATURE_SPEC.md` §1）。
- 今天页全部状态与主按钮逻辑（§2），Snackbar 撤销/改日期，近 10 天确认弹窗。
- 周期圆环组件（`07_UI_UX_GUIDE.md` §5.1）。
- ViewModel 单元测试（用 fake repository + 固定日期）。

### M4 · 日历 + 每日记录
- 月视图、滑动切月、回到今天、图例、月摘要。
- 日期格子所有视觉状态（颜色+形状），TalkBack 描述。
- DayLog Bottom Sheet：全部字段、自动保存、经期开关与合并/拆分规则联动、未来日期限制。

### M5 · 统计页
- 概览卡片、周期柱状图（Canvas 自绘）、经期长度图、症状 Top 5、痛经分布、历史列表、温和提示、空状态。

### M6 · 提醒
- 通知渠道、5 种提醒、隐私模式文案、`publicVersion`。
- WorkManager 调度与重新调度时机；Worker 内重新判断条件。
- 通知权限申请时机与拒绝后的引导；「收不到提醒？」说明。
- 调度逻辑（计算“下一次应该在哪天几点提醒”）抽成纯函数并单元测试。

### M7 · 应用锁与隐私设置
- PIN 设置/修改/关闭、PIN 键盘、生物识别、自动锁定、输错限制、忘记 PIN 流程。
- `FLAG_SECURE` 开关。
- 锁屏时不渲染底层内容。

### M8 · 备份与数据管理
- `BackupCodec`（加密格式）+ JVM 测试（`08_SECURITY_AND_BACKUP.md` §3.3 列出的全部用例）。
- 导出、导入（覆盖/合并）、预览、结果提示。
- CSV 导出。
- 删除全部数据。
- 数据库无法解密的恢复页。

### M9 · 设置、打磨与国际化
- 设置页全部条目；主题切换；一周起始日。
- 关于、隐私政策（与 `PRIVACY.md` 一致）、开源许可、健康声明页面。
- 英文字符串 `values-en` 补齐。
- 应用图标（自适应 + monochrome）、启动页。
- 深色模式、字体 200%、TalkBack 走查；空状态与错误提示完善。
- 关键 UI 流程的 Compose UI 测试（例如：引导 → 记录例假开始 → 日历出现经期）。

### M10 · 发布准备（详见 `11_OPEN_SOURCE_AND_RELEASE.md`）
- release 构建：R8、资源压缩、去日志规则、keep 规则；`assembleRelease` 成功且 App 能正常运行（若有设备）。
- 签名：把生成密钥的命令交给人类执行，等人类填好 `keystore.properties` 后再继续（这一步需要停下来等）。
- `scripts/release.ps1` 发布脚本（§4）：构建、签名校验、最终 APK 权限检查、SHA-256、`release-info.json`、Release Notes。
- `docs/BUILD_FROM_SOURCE.md`（新手指南）、`docs/ANDROID_RELEASE.md`（发布指南）、`SECURITY.md`、`THIRD_PARTY_NOTICES.md` 定稿；README 补充下载、截图与 APK 校验说明。
- debug 专用的「填充虚构示例数据」入口 + 截图；官网素材包 `docs/site/`（§5）。
- 完整走一遍 `10_ACCEPTANCE_CHECKLIST.md`，结果写入 `PROGRESS.md`。
- `versionName = "1.0.0"`（`versionCode` 自动为 10000）。推送 tag 和创建 GitHub Release 前先在汇报中说明，等人类同意。

## 5. 编码规范

- Kotlin 官方代码风格；公共函数和复杂逻辑写 KDoc（中文或英文均可，保持一致）。
- 标识符一律英文；用户可见文字一律在 `strings.xml`。
- Compose：无状态 Composable + 状态提升；每个页面提供 `@Preview`（浅色/深色各一个，使用假数据）。
- 不在 Composable 中直接访问 Repository。
- 不使用 `!!`（测试代码除外）；可空性要明确处理。
- 不吞异常：捕获后要么转为领域错误返回，要么在 debug 下记录（release 日志已被移除，且日志内容不能包含用户数据）。
- 单个文件 ≤ 400 行为宜，超过时拆分。

## 6. 汇报格式（每个里程碑结束时发给人类）

```
## M3 完成：首次引导 + 今天页
- 做了什么：……（3–6 条）
- 如何验证：verifyPrivacy ✅ / testDebugUnitTest ✅（新增 18 个测试）/ lintDebug ✅ / assembleDebug ✅ / 仪器测试：未运行（无设备）
- 截图或说明：……（如有）
- 决策：DECISIONS.md 新增 D-007、D-008
- 遗留问题 / 需要你确认的事：……
- 下一步：M4 日历 + 每日记录
```

## 7. 何时停下来问人

只有以下情况需要停下等待人类回复，其余情况按「更保护隐私、更简单」原则自行决策并记录：
- 需要强制推送、删除远程分支/标签、改写已推送的历史。
- 需要人类提供的东西：签名密钥（M10）、正式的 applicationId 变更。
- 推送 tag、创建 GitHub Release。
- 环境缺失导致无法构建（§1 第 3 条）。

**已确定的事项**：开源，MIT 许可证；通过 GitHub Releases 和官网项目页发布，暂不上架应用商店。详见 `11_OPEN_SOURCE_AND_RELEASE.md`。
