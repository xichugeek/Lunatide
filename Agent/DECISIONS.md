# 技术决策记录（由 Codex 维护）

> 规范没有写清楚、或你偏离了规范时，在这里记录。编号递增，不删除旧记录（被推翻的标注「已废弃，见 D-xxx」）。

<!-- 模板：
## D-001 · 标题（YYYY-MM-DD）
- 背景：
- 选项：A … / B …
- 决定：
- 理由（隐私 > 简单 > 体验）：
- 影响：
-->

## D-000 · 初始约定（规范作者预置）
- applicationId：`com.xichugeek.lunatide`
- 单 app 模块；预测引擎纯 Kotlin。
- 数据库加密使用 SQLCipher + Keystore 包裹密钥；PIN 只作界面层访问控制。
- 不引入任何联网能力；图表自绘；开源许可页手写。
- 开源许可证：MIT，`Copyright (c) 2026 西楚极客 XichuGeek`（与作者其他开源项目一致）。
- 发布渠道：GitHub Releases + 官网项目页；暂不上架应用商店；App 不检查更新。
- versionCode = MAJOR×10000 + MINOR×100 + PATCH。

## D-001 · 修复 SDK 与稳定工具链（2026-10-24）
- 背景：初次检查只发现 SDK Platform 36.1；用户明确授权自行修复开发环境并继续。
- 决定：使用官方工具安装稳定 Platform 37.2，采用 AGP 9.4.1、Gradle 9.8.2、Kotlin 2.4.21、KSP 2.3.12、Hilt 2.60.1。AGP 内建 Kotlin，根脚本显式提升 Kotlin Gradle Plugin 到当前稳定版本；不使用旧 DSL 退出选项。
- SDK：compileSdk 使用 API 37 minor 2；targetSdk 使用基础 API 37；minSdk 26。编译运行时可用 JBR 21，源码 Java/Kotlin toolchain 与字节码为 17。
- SDK 命令行工具、Gradle 从官方源下载并验证官方摘要；JDK 由 Adoptium 官方 HTTPS API 下载。它们均不提交仓库。Android CLI 显式传入 `--no-metrics`。
- 来源：https://developer.android.com/build/releases/agp-9-4-0-release-notes 、https://developer.android.com/build/migrate-to-built-in-kotlin 、官方 Maven 元数据与 SDK repository2-3.xml。

## D-002 · 传递依赖的纯注解例外（2026-10-24）
- 背景：AndroidX、Hilt 的传递依赖包含 `javax.inject:javax.inject`、`org.jetbrains:annotations`、`org.jspecify:jspecify`，它们不在规范的运行时 group 列表中。
- 决定：仅对上述精确坐标允许依赖白名单例外；仍对所有传递依赖执行黑名单扫描与报告。主代码禁用 API 豁免仍为零。
- 理由：这些库仅提供注入/空值等注解，无联网或数据采集能力；避免对整个第三方 group 放行。
- 建议修改规范：03 依赖白名单补充这些必需的纯注解传递依赖。
- 首次完整解析另发现 `jakarta.inject:jakarta.inject-api:2.0.1`、`com.google.code.findbugs:jsr305:3.0.2`（注解）、`com.google.guava:listenablefuture:1.0`（单个 Future 接口）；已检查缓存 JAR 内容，仅精确放行这些坐标，不放行完整 Guava。它们无联网或采集功能。

## D-003 · M0 工具提示的处理（2026-10-24）
- Lint 为 0 error、1 warning：`mipmap-anydpi-v26` 的 v26 限定对 minSdk 26 是冗余的。保留以遵循 03/07 文档的图标目录结构；不影响资源解析与隐私。
- Manifest merger 提示网络权限的 remove 标记目前没有匹配项；按 R1 保留以防未来依赖引入网络权限。
- Gradle 提示部分插件使用将在 Gradle 10 删除的 API；当前稳定版本构建成功，不修改第三方插件或启用旧 DSL 绕过。

## D-004 · 数据层细节（2026-10-24）
- 体温统一用整数百分度，日期显式转换为 epochDay，无需隐式 Room TypeConverter，避免浮点误差和时区变化。
- 周期长度参数按 05 文档许可放私有 DataStore；健康记录只进加密数据库。未知枚举 code 映射为 null/忽略。
- SQLCipher 工厂经 DI 注入。数据库在 IO 线程首次实际打开后清零调用方口令缓冲区；不启用 Room 自动关闭，显式关闭后必须由 DatabaseOwner 重新解包密钥并创建新实例。
- SQLCipher 的连接池需要口令工作副本以创建后续连接：工厂为其保留独立副本，关闭 helper 时清零；不能在第一次打开后把连接池所用口令一并清零。调用方缓冲区仍在首次打开后立即清零。
- SQLCipher Java 日志切换为 NoopTarget；不打印异常、口令、记录或 SQL 参数。源代码的禁用 API 豁免保持为零。
- StrongBox 不可用回退普通 Keystore；已有库或包裹密钥损坏时返回 DataUnavailable，绝不静默重置。
- 日历中间日取消经期必须显式确认缩短，不提供拆分。被移出的日期同步清除非点滴经量，并清理非经期空记录。

## D-005 · DataStore 的 Okio 本地文件传递依赖（2026-10-24）
- DataStore 1.3.0 传递引入 `com.squareup.okio:okio-jvm:3.17.0`，用于本地文件读写；不是 OkHttp，不是统计、广告或采集 SDK。App 不调用其网络适配器，最终 Manifest 依旧无网络权限。
- 仅对白名单增加 `com.squareup.okio:okio` 和 `com.squareup.okio:okio-jvm` 精确坐标例外，不放行整个 Square group；禁用 API 与网络依赖黑名单不变。
- 选择保留官方最新稳定 DataStore 所需的文件层，而不是降级 DataStore 或从传递依赖中盲目排除后造成运行时崩溃。建议在 03 文档白名单中说明此必需传递依赖。

## D-006 · 预测统计边界（2026-10-24）
- 标准差使用总体标准差（除以 n）；离群过滤偶数中位数取中间两个数的算术平均，正数取整用 roundToInt。
- 增加配置 showFertility，保留原接口其余默认值；预测最多 6 个周期，手动参数也受规范范围限制。
- 系统日期倒退时，预测只采用开始日不晚于今天的历史，不修改原数据。未来日期不展示预测的过去片段。
- 症状、痛经分布取最近 6 个周期开始至今天；所有温和提示均需至少 3 个有效周期。
- 历史先按开始日排序（O(n log n) 归一化，满足规范要求的升序处理）；之后的统计/预测为 O(n)。对不超过 300 条历史保持实现简单，并用 20 年虚构数据测试实际耗时，不为此引入排序依赖。

## D-007 · 首页完成态与分阶段入口（2026-10-24）
- 04 今天页状态表将结束日当天视为经期；10 的 B2 要求点“例假走了”后显示等待下次。首页按是否存在进行中记录显示完成态，优先满足 B2；日历与预测仍保留结束日属于真实经期的日期语义。
- 进行中记录超过显示上限时，首页继续提供结束入口并提示确认结束日期，避免用户无法结束；日历与统计的有效区间仍按 start+14 上限处理。
- M3 的引导第 4 页先展示可选安全/提醒说明；提醒启用与权限申请在 M6 接通，PIN 设置在 M7 接通。每日记录入口先可查看摘要，全部编辑字段在 M4 接通；设置全部条目在 M9 完成。最终验收前必须补全这些入口，不将其当作已实现的安全/提醒/编辑功能。
- 首页 Snackbar 同时提供撤销和改日期；改日期基于操作前经期快照在单个事务中重新应用，保留其他每日记录。

## D-008 · 显式中文资源（2026-10-24）
- 设备走查发现：仅默认 values 中文与 values-en 英文时，设置 App 语言 zh-CN 后仍可能优先命中系统第二语言英文，因为资源语言列表缺少显式 zh。
- 构建时从默认 strings.xml 自动生成 values-zh，作为带任务依赖的资源目录；中文文案仍只有一个维护源，生成物不提交。英文保持 values-en。
- 日期格式使用当前配置的 Locale，而非假定 JVM 的全局语言。

## D-009 · 每日记录与日历细节（2026-10-24）
- 经量关联经期开关的复合编辑以 Room 外层事务包裹经期编辑和日志保存，任何失败完全回滚；近 10 天和中间日缩短始终先确认。
- “清空这天的记录”清空每日感受字段，保留经期日期；需要移出经期时单独使用开关，避免误改历史。
- 自动保存防抖 300 ms，关闭 Sheet 先取消待执行任务并写入最新草稿；保存失败保留草稿和错误提示。无效体温阻止关闭，需修正或清空。
- 自由输入使用 AndroidView/EditText，关闭联想与纠错、设置 IME_FLAG_NO_PERSONALIZED_LEARNING、禁用 View 状态保存，草稿不进入 SavedStateHandle 或 rememberSaveable。
- 日历已记录经期的浅色填充采用更深玫瑰色 #B53E57 搭配白字，以满足文字对比度；品牌 #E0637A 保留给圆环等无文字装饰。深色经期采用浅玫瑰色与深色文字。建议 07 文档明确语义填充与装饰色的区别。
- 日历格子按 7 列自适应，触区高度至少 56 dp；页面横边距 12 dp，为窄屏保留更多宽度，其他页面维持 20 dp。

## D-010 · CI SDK 安装修复（2026-10-24）
- GitHub 历史推送构建在 setup-android v3 默认安装已移除的 `tools` 包时失败，尚未执行项目 Gradle 检查；本地成功不能代表 CI 通过。
- 移除该旧安装步骤，直接从 Google 官方下载固定的 Command-line Tools 23.0 Linux 包，校验官方索引中的摘要，并用新的 Android CLI（--no-metrics）显式安装 Platform 37.2 和 AGP 所需 Build Tools 36.0.0。
- CI 同时安装 JDK 17 与 21：21 运行构建工具，17 编译源码，与本地验证组合一致；保持四项隐私/单测/Lint/APK 命令不变。经各官方仓库 latest release 核对，锁定 checkout 7.0.1、setup-java 6.1.0、gradle/actions 6.4.0。

## D-011 · 统计图与历史（2026-10-24）
- 图表展示最近 12 个实际完整周期/已结束经期，均值遵循 06 的有效记录过滤；不以预测代替实际统计。图表用 Canvas 绘制，每根柱子提供独立语义描述，可横向滚动。
- 历史显示全部周期，最新周期显示实际进行天数；点击跳转日历对应月份。症状与痛经分布只统计最近 6 个周期。
- 空状态以经期记录少于 2 次判断；规律性不足 3 个有效周期时明确显示数据不足，不猜测规律性。
