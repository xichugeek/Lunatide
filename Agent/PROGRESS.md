# 开发进度（由 Codex 维护）

> 每个里程碑结束时更新。最新的放在最上面。不要删除历史记录。

## 当前状态

- 当前里程碑：M2 已完成，准备 M3 首次引导与今天页
- 最近一次构建：`verifyPrivacy testDebugUnitTest lintDebug assembleDebug` 成功（2026-10-24）；累计 61 个 JVM 测试通过。M1 的 5 个仪器测试通过，本次纯 Kotlin 变更未重复执行
- 阻塞问题：无；原 SDK 缺失已自行修复

## 环境

| 项 | 结果 |
|---|---|
| 操作系统 | Windows NT 10.0.26100.0；PowerShell 7.6.5；检查日期 2026-10-24（Asia/Shanghai） |
| JDK | Gradle 运行于 Android Studio JBR 21.0.9；另安装 Temurin JDK 17.0.20.1，`jvmToolchain(17)` 已通过真实编译与 JVM 测试验证。 |
| Android SDK 路径 / 已安装平台 | SDK 为 `D:\Android\Sdk`；Platform 36.1、37.2；Build Tools 36.0.0、36.1.0、37.0.0；Command-line Tools 23.0。已创建被 Git 忽略的 `local.properties`。 |
| Gradle / AGP / Kotlin 版本 | 已锁定并构建验证：Gradle 9.8.2、AGP 9.4.1、Kotlin 2.4.21、KSP 2.3.12、Hilt 2.60.1；其余版本见 Version Catalog。 |
| 依赖下载是否正常 | Google Maven、Maven Central 与 Gradle 官方分发实际下载成功；M0、M1 完整依赖解析、编译、测试和 APK 构建成功。 |
| 可用设备（adb devices） | 已启动 Pixel_7 / emulator-5554，Android API 35，1080×2400；WHPX 可用；M0 启动导航走查与 M1 的 5 个仪器测试通过。没有验证其他 API 或实体手机。 |

### 环境检查依据与恢复步骤（2026-10-24）

- 已完整阅读 `README.md`，并严格按顺序完整阅读 01～11 号文档，再读取 `PROGRESS.md`、`DECISIONS.md`；读取完成前没有写代码。
- 官方 SDK 索引 `https://dl.google.com/android/repository/repository2-3.xml` 返回 `platforms;android-37.0`、`platforms;android-37.1`、`platforms;android-37.2`；其中 37.2 位于 stable channel（`channel-0`）、无 codename、无 preview revision。不要选择另列的 beta 包。本机仅有 36.1 平台；已安装 37.0.0 Build Tools 或 37.0 系统镜像不等于安装了 SDK Platform。
- 请在 Android Studio → Tools → SDK Manager，确认 SDK Location 为 `D:\Android\Sdk`，在 SDK Platforms 中安装当前稳定 Android API 37 平台（本次官方索引列出的最新稳定包为 Android SDK Platform 37.2）；在 SDK Tools 中安装 Android SDK Command-line Tools (latest)，便于后续工具链检查。
- 安装完成后重新检查 SDK，再继续 M0 的 Git 初始化、工程与隐私检查任务。设备不是构建的前置阻塞：后续可尝试启动已有 Pixel_7；无法运行时设备验收如实标记未验证。
- Git 可用；`git ls-remote https://github.com/xichugeek/Lunatide.git` 成功且没有返回任何 refs；本地尚不是 Git 仓库。没有提交、推送、创建标签或 Release。

## 里程碑记录

### M2 · 预测引擎与统计 —— ✅ 完成（2026-10-24）
- 完成内容：纯 Kotlin 预测/统计、最近 12 个周期硬过滤与中位数离群过滤、最近 6 个加权平均、先验/手动优先、置信度和范围、进行中上限、固定原始预测与推迟顺延、易孕期裁剪与隐藏原因、症状 Top 5、痛经分布、规律性和温和提示。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 31，累计 61 tests，0 failures/errors）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅；仪器测试本次未重复执行（纯 Kotlin 计算变更，M1 结果保留）。
- 验收清单：A1～A5 ✅；C1 ✅（06 §8 全部 25 个场景按 t01～t25 对应）；C3～C4 计算逻辑 ✅（UI 待 M3/M4）；C5 固定样例计算 ✅（统计页待 M5）；G8 ✅。20 年虚构数据测试为 260 次经期、7300 条日志，计算可完成；滚动流畅度尚未验证。
- 决策：D-006（标准差、日期倒退、数据范围与排序复杂度说明）。
- 提交：本次 `feat(prediction): 实现周期预测与统计并覆盖规范场景`。
- M1 提交与推送：`da79d2c` 已成功推送 `origin/main`。
- 遗留问题：上述预测还未接入 UI；不能以 JVM 性能结论替代中端真机体验验证。
- 下一步：M3 首次引导、今天状态卡、操作撤销/改日期与 ViewModel 测试。

### M1 · 数据层 —— ✅ 完成（2026-10-24）
- 完成内容：三张 Room 表与 DAO、显式领域映射、稳定枚举、百分度体温、schema v1 导出；SQLCipher + Keystore AES-GCM 包裹随机口令；可注入工厂、非破坏性错误；DataStore 设置与可注入 TodayProvider；周期/日志 Repository 与事务化经期编辑。
- 测试：新增 30 个 JVM 测试覆盖 05 §3 的新建、进行中限制、相邻扩展、桥接合并、历史补录、近 10 天确认、首尾/中间/单日删除、未来日期、体温备注边界、跨年闰年等；5 个真实仪器测试覆盖 SQLCipher 读写重开、错误口令、非明文文件头、密钥丢失/包裹文件缺失保留数据库、经量清理保留点滴、内存 Room 与事务回滚。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（30 tests, 0 failures）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（5 tests, 0 failures/0 errors/0 skipped，Pixel_7 API 35）。
- 验收清单：A1～A5 ✅；B5 领域规则 ✅（日历 UI 尚未实现，完整条目待 M4）；E1～E2 ✅；E8 数据保留逻辑 ✅（恢复 UI 待 M8）；G8 ✅；其他 UI/提醒/锁/备份/发布项待对应里程碑。
- 决策：D-004、D-005。DB 口令调用方缓冲区及时清零，连接池工作副本在关闭时清零；不以清零破坏连接池后续连接。
- 提交：本次 `feat(data): 实现加密数据层与经期编辑规则`；待推送结果补记。
- M0 提交与推送：`3b52626` 已成功推送 `origin/main`。
- 遗留问题：仪器测试仅覆盖 API 35 模拟器；其他设备与完整功能验收后续完成。
- 下一步：M2 预测引擎与统计，覆盖 06 §8 全部 25 个场景。

### M0 · 工程初始化与隐私防线 —— ✅ 完成（2026-10-24）
- 完成内容：Git/main/远端；Gradle Wrapper（分发 SHA-256）、Version Catalog、单 app、Compose/Material 3/Hilt、类型安全三个导航占位；Manifest 与备份排除加固；三个隐私任务并接入 check；push/PR CI；AGENTS、README、PRIVACY、MIT LICENSE、SECURITY、CHANGELOG、两个 Issue 模板；版本号自动计算；初版矢量自适应图标。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（任务 NO-SOURCE，0 个测试，业务测试从 M1 开始）/ `lintDebug` ✅（0 error、1 warning，D-003）/ `assembleDebug` ✅。
- 故意失败验证：临时将 INTERNET 从移除标记改为实际权限；`verifyPrivacy` 以“debug 出现未授权权限 android.permission.INTERNET”失败，退出码非零。finally 恢复 Manifest，再运行四项检查全部成功。未安装或提交含网络权限的 APK。
- 设备：Pixel_7 / emulator-5554 / API 35 安装 debug APK 成功，冷启动 Status ok；UI 层级确认日历与统计导航页。尚未运行仪器测试套件（M0 未编写）；首次 debug 启动命令 TotalTime=2247 ms，不据此宣称满足正式版冷启动目标。
- 验收清单：A1～A5 ✅（两个最终 Manifest、全部备份排除域、依赖清单人工检查、源码无用户日志）；A6 ⏭（M10）；A7～A8 ⏭（完整权限设置与飞行模式功能走查留待功能就绪）；G8 ✅（warning 说明见 D-003）；G9 ✅（当前导航界面提供浅/深色 Preview）；H3、H6 ✅；其余业务与发布项未实现，不标记通过。
- 决策：D-001～D-003。依赖报告位于 `app/build/reports/privacy/dependencies.txt`（构建产物不提交）。
- 截图或说明：模拟器实机界面已检查，仅为空导航占位；截图未作为正式宣传素材。debug APK 约 11.39 MiB，不作为 release 体积结论。
- 遗留问题：完整业务、设备验收、release 构建与签名按后续里程碑实施。
- 提交：本次 `chore(build): 初始化安卓工程与隐私防线`；推送结果在后续日志补记。
- 下一步：M1 数据层。

### 环境修复与 M0 恢复 —— 🚧 进行中（2026-10-24）
- 用户明确授权自行修复环境；已安装官方 SDK Command-line Tools 23.0、稳定 SDK Platform 37.2、Temurin JDK 17.0.20.1；JBR 21 继续作为 Gradle 运行环境。工具均在仓库之外。
- Gradle 9.8.2 下载并按官方 SHA-256 校验，生成包含分发摘要的 Wrapper。命令行工具按 SDK 官方索引 SHA-1 校验。Android CLI 使用 `--no-metrics`，SDK 许可证已存在。
- SDK 新版 `sdkmanager` 会转交 Android CLI，旧参数未安装平台；随后依据 `android sdk install --help` 使用新版命令完成安装。
- 启动已有 Pixel_7，`adb devices -l` 返回 `emulator-5554 device`，`sys.boot_completed=1`；Android API 35，1080×2400。仅验证设备启动，App 功能尚未验证。
- Git 已初始化 main，配置指定远端并 fetch（远端无 refs）；提交身份使用项目公开别名与 GitHub noreply 地址。
- 已创建 M0 工程、Manifest 加固、三个隐私检查、CI 与开源基础文档，开始运行四项检查。尚未提交或推送。
- 决策：D-001、D-002。

### M0 · 工程初始化与隐私防线 —— ❌ 阻塞（2026-10-24）
- 完成内容：规范完整阅读；JDK、SDK、仓库端点、设备及 Git 远端只读检查；将事实和恢复步骤记录在本文件。
- 尚未实现：工程、隐私任务、CI、页面及其他 M0 交付物。没有开始 M1～M10。
- 验证：`verifyPrivacy` ⏭ / `testDebugUnitTest` ⏭ / `lintDebug` ⏭ / `assembleDebug` ⏭（均未运行：环境检查发现所需 SDK Platform 缺失，尚无 Gradle 工程）；仪器测试 ⏭（无已连接设备）。
- 验收清单：A1～A4 ⏭（尚无工程与合并 Manifest / 依赖报告）；A5 ⏭（尚无 App 源码）；A6 ⏭（M10）；A7～A8 ⏭（设备未验证）；G8～G9、H6 ⏭（M0 尚未实现）。其余功能项未实现，不标记通过。
- 决策：无新增实现决策；依照规范停止，不自行降级 SDK。
- 遗留问题：需人类补齐 SDK Platform 后继续。
- 提交：无；仅更新本进度文件，M0 未达到提交并推送的完成条件。

<!-- 模板：
### M0 · 工程初始化与隐私防线 —— ✅ 完成 / 🚧 进行中 / ❌ 阻塞（YYYY-MM-DD）
- 完成内容：
- 验证：verifyPrivacy ✅ / testDebugUnitTest ✅ (N tests) / lintDebug ✅ / assembleDebug ✅ / connectedAndroidTest ⏭（原因）
- 验收清单：A1 ✅ A2 ✅ …
- 遗留问题：
- 提交：<commit hash 简写>
-->

## 待人类确认

- [x] 开源许可证：MIT（已确定）
- [x] 发布渠道：GitHub Releases + 官网项目页（已确定）
- [ ] release 签名密钥（M10 时由人类生成）
- [ ] 官网项目页最终 slug（计划 `/projects/lunatide/`）
