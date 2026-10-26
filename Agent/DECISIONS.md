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

## D-012 · 提醒调度与附加隐私加固（2026-10-24）
- 采用 WorkManager 2.12.0 与 Hilt 注入的自定义 WorkerFactory，不引入额外集成库。移除默认初始化器以使用 Application 的 Configuration.Provider；Worker 输入仅含提醒类型，不放健康日期、备注或症状。
- 无网络约束、无精确闹钟、无前台任务；移除网络、前台服务权限与未使用的前台服务组件。保留系统签名权限 BIND_JOB_SERVICE 保护的 SystemJobService 导出，这是系统调度必需的入口；建议 08 的“其他组件一律不导出”明确此系统组件例外。额外 Manifest 检查只允许该例外和 Launcher。
- 移除 EmojiCompat 自动字体加载初始化器、ProfileInstallReceiver、DiagnosticsReceiver 和 debug PreviewActivity，缩小隐式外部行为与导出入口；IDE 静态 Compose Preview 保留。
- 隐私模式默认开启，所有实际通知 visibility=PRIVATE 并提供固定中性 publicVersion；库日志阈值仅设为 ERROR（Log.ERROR 为常量，非输出用户数据的日志调用），release 继续用 R8 去日志。
- 到点后台冷启动时，保留已经到期的任务，防止启动时重新调度误取消通知；运行后的重算放到独立延迟 1 秒的任务，避免替换正在执行的自身任务。易孕期提醒按未被“今天”截断前的首个有效日期判断，避免每天重复提醒。
- 引导全部草稿放 ViewModel 内存，日期选择器采用 remember + DatePickerState 工厂，不使用保存日期的 rememberSaveable；旋转保留引导草稿，进程结束不落系统状态存档。已完成记录仍只在 SQLCipher 中保存。
- Lint 对 ProfileInstallReceiver 的移除标记报告 MissingClass；仅对此 tools:node=remove 节点局部忽略。最终 Manifest 中该组件确实不存在；未对实际注册组件关闭 MissingClass 检查。
- 源码检查额外禁止直接使用记忆状态存档 API（rememberSaveable、SavedStateHandle），防止后续健康字段误写系统状态；当前手写 UI 状态均采用 ViewModel/内存，不需要非健康字段豁免。

## D-013 · 应用锁与 PIN 持久化（2026-10-24）
- 使用 AndroidX Biometric 稳定版 1.1.0（官方稳定版本表核对，https://developer.android.com/jetpack/androidx/releases/biometric）与 Lifecycle Process 2.11.0；优先 STRONG，仅在不可用时回退 WEAK。绝不允许 DEVICE_CREDENTIAL；无 PIN 无法启用生物识别。
- PIN 使用 16 字节随机盐、PBKDF2-HMAC-SHA256 120000 次、32 字节哈希；运算移至 Default，比较使用 MessageDigest.isEqual。PIN 草稿仅在内存，自绘键盘不调用输入法、不播放按键音；取消/完成时清除确认缓冲。
- security.preferences_pb 单独保存哈希和错误次数、剩余冷却、uptime 锚点；记录启动时间估计以检测重启。重启/时钟大改时保守重计上次保存的剩余时长，绝不因输错清除数据；冷却期间最多每 5 秒保存一次检查点。
- ProcessLifecycleOwner 作为进程锁定依据，同时 Activity 在真正离开（排除配置变化）时提前设置锁状态，避免立即锁定受 ProcessLifecycleOwner 的延迟派发影响。外部跳转豁免只用于应用主动发起的系统权限/设置/文件选择器，并在返回时清除。
- 双存储未读取完成时访问门默认关闭；锁定时不组合底层导航/记录界面。开启应用锁默认开启 FLAG_SECURE，窗口创建初始即保护，读取到明确关闭设置后才解除。
- 忘记 PIN 仅提供两次确认后清除数据，无恢复 PIN 或跳过入口；英文确认词使用 DELETE，中文使用删除。删除与后续 M8 数据页共用清理器，成功后以新 Activity/全新 ViewModel 返回引导。
## D-014 · 备份边界与恢复安全（2026-10-24）
- 备份 v1 固定导出 PBKDF2-HMAC-SHA256 600000 次，42 字节头部全部作为 GCM AAD；导入先校验格式/长度与 KDF 上界（100 万），输入文件最多 16 MiB、解压后最多 32 MiB，避免恶意文件耗尽资源。测试构造低迭代对象，产品只调用固定默认构造。
- 备份 settings 使用独立白名单 DTO，排除 PIN、锁、FLAG_SECURE 和密钥。JSON 忽略未知字段，枚举字段逐个丢弃未知值；非法日期/未来健康记录拒绝整个导入，先校验再写入。
- 覆盖和合并的全部健康记录写入一个 Room 事务；同起始日期的经期优先较新版本，再合并重叠/相邻区间；同日记录保留较新整条。设置属于独立 DataStore，在数据库事务提交前更新，失败时尝试恢复旧设置；不宣称跨两个存储存在系统级原子事务，健康数据事务失败会完全回滚。
- 数据库无法解锁时原文件保留。只有用户成功解密备份并明确确认覆盖后，才在 no_backup 内创建新的 SQLCipher 数据库，写入成功并 checkpoint/close 后以持久化标记完成两次原子重命名；启动检测标记继续完成替换，防止半途替换 DB/包装密钥。准备失败保留原数据库文件，暂存文件始终加密，无明文健康数据临时文件。
- 所有文件导入导出仅走 SAF，不请求存储权限；应用不主动选择云提供商，界面解释系统选择器提供的位置由用户决定。文件返回采用一次锁定豁免，密码与预览仅留 ViewModel/内存，进程结束后需重新选择。
- CSV UTF-8 BOM、CRLF 和 RFC 4180 转义；用户文本以 =/+/-/@ 等公式前缀开头时加单引号防表格公式执行，保留其可读内容。默认不导出同房列；密码输入关闭联想/个性化学习，不持久化视图状态。
## D-015 · 中文体验与视觉打磨（2026-10-24）
- 用户明确要求中文界面，并要求针对女性日常使用打磨 UI。交付模拟器固定切回 zh-CN，不将英文兼容测试后的状态留给用户；英文完整资源保留。顶部简洁显示月日和星期，主要状态采用清晰的大数字、等宽数字特性与温和说明。
- 首页采用暖白背景、月夜靛主卡、柔和玫瑰/潮汐青的周期段、月相标记；按钮与状态置于同一卡片，减少视线跳转。取消 Material 默认偏紫的整片卡片底色，统一白色/深色 surface、圆角和留白；设置使用简洁列表卡片，日历使用箭头和窄星期标题，所有图标有语义。
- 圆环位置用 600ms Compose 动画（遵循系统动画比例）；大字号时改为自然高度的文字状态，避免把 200% 字号强塞入固定圆环。日期数字保留单行与等宽数字；步进器可换行。
- 使用 core-splashscreen 1.2.0。启动页统一使用月夜靛品牌背景与白色月汐图标，应用设置读取期间也使用同色，避免手动深色主题下先闪白。未使用基于位置/传感器的自动夜间模式，也不改变系统主题；跟随系统由 Compose 当前配置判断。此启动背景是对 07 中通用 background token 的品牌化细化。
- 隐私政策与开源许可作为根目录单一来源，由构建复制到只读 assets，应用不维护另一份可能漂移的中文文本；英文政策另列 PRIVACY.en.md。许可列出所有解析后的运行时坐标及版本，verifyDependencies 增加遗漏即失败；完整保留 SQLCipher Android 4.19.1 官方 BSD 许可全文和 MIT/Apache 2.0 文本。
- 文本输入另外禁用自动填充与支持系统的内容捕获；API 边界通过 Lint 检查，不扩大权限。手动经期长度 1–14 天，与预测引擎范围保持一致。
- Lint 保留 localeConfig 的 UnusedAttribute 提示：该配置只供 Android 13+ 的系统应用语言选择使用，旧系统忽略该属性，符合兼容设计；不使用全局忽略。另一条图标目录提示仍按 D-003 保留。
## D-016 · Release 实测、示例隔离与发布门槛（2026-10-24）
- 首版版本 1.0.0，versionCode 自动为 10000。未提供正式签名时，只允许本地 debug 证书验证 R8 包，发布脚本必须拒绝缺失配置或 Android Debug 证书，绝不把本地测试指纹当正式指纹。
- 本机 Build Tools 37 的 apksigner 将证书前缀输出为 V2 Signer，发布脚本兼容该格式及旧 Signer #1 格式，只公开 SHA-256，不输出证书主体个人信息或密码。
- full lintRelease 发现 release 不含 PreviewActivity 时，公共 Manifest 中的移除标记触发 MissingClass；将该移除标记移到 debug Manifest，仍由最终 Manifest 导出检查保证不能注册 PreviewActivity，不使用全局忽略。
- R8 实机验证发现，路由字符串与运行时 KClass.qualifiedName 比较会受混淆影响而隐藏底部导航；改为官方 NavDestination.hasRoute 类型检查，并再次实际验证 release 三个 Tab。
- 示例生成、虚构记录与入口文案仅在 src/debug；src/release 的 DeveloperEntry 为空插槽，release 的 DEX/resources 再次扫描不得包含样例入口。另提供 debug 专用的 260 次经期/7300 天记录性能数据，正式截图始终使用约 8 个周期且无同房记录。
- 日历把日志列表按日期索引并在数据变化时缓存，避免每个日期格都遍历 20 年日志。正式冷启动计时在记录就绪并完成两帧后 reportFullyDrawn，不把 Activity 首帧时间当成完整可交互时间；模拟器结果与实体机目标分开记录。
- 设置、提醒、安全和每日记录的开关统一使用整行 48dp 以上触点；文字与开关合并为一个带状态的无障碍节点，避免只能点右侧小开关及重复朗读。无 PIN 时的生物识别仍保持整行禁用。
- 截图为 API 35 模拟器、1080×2400、简体中文、浅色、虚构数据；临时把模拟器时钟设为 2026-10-12，以在同一月网格显示实际经期、未来易孕期与预测经期。完成验证后恢复真实时钟、100% 字号和中文，图像不修饰或伪造。
- 官网文案有完整 12 节草稿和模板；正式签名的 release-info.json 只能由真实 APK 生成。签名未配置时不创建伪造元数据，不填假哈希；update-site.ps1 在缺少真实信息时停止。签名与标签/Release 分两步交给人类确认。

## D-017 · 用户授权代执行签名设置（2026-10-24）
- 用户明确要求“我已经打开了你帮我执行一下”，授权代为启动前一轮给出的签名命令；不再要求用户重复复制命令。使用独立可见的本机终端运行 setup-signing.ps1，由用户本人直接输入密码，不通过聊天、代理工具参数或日志传递密码，也不替用户生成密码。
- 脚本预填项目公开证书主体 CN=Lunatide / OU=Open Source / O=XichuGeek / C=CN；正式密钥位于仓库外，PKCS12 密钥密码与库密码一致。已有密钥不重建，已有配置不覆盖。用户再次输入并验证密码成功后写入已忽略的 keystore.properties，按 Java Properties 规则处理空白、反斜杠与 Unicode。
- 此授权只覆盖本机签名设置和原任务已授权的构建验证，不包含 tag、GitHub Release 或网站部署；这些公开发布步骤仍需最终产物可审阅后获得单独同意。

## D-018 · 正式签名恢复与独立验收（2026-10-26）
- 关机后恢复开发环境，用户在本机交互窗口完成签名设置。只检查文件存在并通过签名构建验证可用，不读取或输出密码；证书指纹可公开，私钥和本机配置继续忽略。
- 首次正式脚本在构建成功后因新版 aapt2 使用 minSdkVersion 而非旧 sdkVersion 字段停止；兼容两种精确字段，缺失字段明确失败，仍要求 minSdk=26、targetSdk>=37，不降低校验标准。
- 安装和升级验收使用独立测试 AVD，避免卸载或覆盖原模拟器里的测试签名 App 及其记录。已有数据保留，正式证书包不尝试跨签名覆盖。
- 首次生成官网文档后 git diff --check 发现 Windows Set-Content 追加 CRLF 和多余空行；发布文本与官网文本统一 UTF-8 无 BOM、LF、一个末尾换行，保持元数据内容与实际签名 APK 同源，校验文件格式跨平台一致。
