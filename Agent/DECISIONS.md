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
- 工具从官方源下载并验证官方摘要；不把 SDK、JDK 或临时构建工具提交仓库。Android CLI 显式传入 `--no-metrics`。
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
