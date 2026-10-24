# 03 · 技术栈与架构

## 1. 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 语言 | Kotlin（最新稳定版，2.x） | 不写 Java 新代码 |
| UI | Jetpack Compose + Material 3 | 单 Activity；不使用 XML 布局（Manifest、资源、主题除外） |
| 导航 | Navigation Compose（类型安全路由，使用 `@Serializable` 路由对象） | |
| 依赖注入 | Hilt（KSP） | |
| 数据库 | Room（KSP）+ SQLCipher for Android | 加密数据库，见 `08_SECURITY_AND_BACKUP.md` |
| 设置存储 | DataStore Preferences | 只存非健康数据的设置项；PIN 的哈希也存这里 |
| 异步 | Kotlin Coroutines + Flow | 不用 RxJava、LiveData |
| 后台提醒 | WorkManager | 不需要网络约束；移除其 `ACCESS_NETWORK_STATE` |
| 生物识别 | androidx.biometric | |
| 日期 | `java.time`（minSdk 26 原生支持） | 全程使用 `LocalDate`，不处理时分秒和时区 |
| 序列化 | kotlinx.serialization（JSON） | 用于备份文件和导航路由 |
| 图表 | **自己用 Compose Canvas 画** | 不引入图表库 |
| 测试 | JUnit4/5、kotlinx-coroutines-test、Turbine、Truth 或 AssertK、Robolectric（仅测试）、Compose UI Test | |

**版本策略**：一律使用当前**最新稳定版**（不用 alpha/beta，除非稳定版缺少必须的能力，此时写进 `DECISIONS.md`）。所有版本集中在 `gradle/libs.versions.toml`（Version Catalog）。Compose 用 BOM 管理。

**SDK**：`minSdk = 26`，`compileSdk = targetSdk =` 当前最新稳定 API Level（不低于 35）。JDK 17（或 Android Studio 自带的 JBR 21），`jvmToolchain(17)`。

## 2. 依赖白名单

只允许以下 group 的依赖进入 `implementation`/`runtimeOnly`：

- `org.jetbrains.kotlin`、`org.jetbrains.kotlinx`（coroutines、serialization、collections-immutable）
- `androidx.*`（core、activity、lifecycle、compose、navigation、room、datastore、work、biometric、hilt-navigation-compose、core-splashscreen 等）
- `com.google.dagger`（Hilt）
- `net.zetetic`（sqlcipher-android）
- `com.google.android.material`（仅当主题需要时，用于 XML 主题父类；尽量不用）

测试依赖（`testImplementation`/`androidTestImplementation`）可额外使用：`junit`、`org.robolectric`、`app.cash.turbine`、`com.google.truth` 或 `com.willowtreeapps.assertk`、`io.mockk`（尽量少用 mock，优先 fake）。

构建插件：AGP、Kotlin、KSP、Hilt、kotlinx-serialization、Compose Compiler 插件。不引入 detekt/ktlint 等额外插件（Android Lint 足够），除非写进 `DECISIONS.md`。

**禁止**：Google Play Services、Firebase、`oss-licenses-plugin`（它依赖 GMS；开源许可页面手写）、任何网络库、任何图片加载库（App 不需要加载网络图片）。

## 3. 工程结构

采用**单 app 模块 + 清晰分包**（项目规模不需要多模块，降低构建复杂度）。预测引擎必须是**纯 Kotlin、无 Android 依赖**，便于 JVM 单元测试。

```
Lunatide/
├── AGENTS.md                    # 给 AI 代理的简要入口（由你在 M0 创建，指向 Agent/README.md）
├── README.md                    # 面向用户/开发者的项目介绍（中文）
├── PRIVACY.md                   # 隐私政策（与 App 内一致）
├── Agent/                       # 需求与规范（本目录）
├── .github/workflows/android.yml
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── gradle/wrapper/...
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── privacy-allowlist.txt    # 预期为空
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/xichugeek/lunatide/
        │   │   ├── LunatideApp.kt              # @HiltAndroidApp
        │   │   ├── MainActivity.kt             # 单 Activity，处理 FLAG_SECURE、应用锁
        │   │   ├── core/
        │   │   │   ├── time/                   # Clock 抽象（可注入，测试可固定“今天”）
        │   │   │   ├── security/               # 数据库密钥管理、PIN 哈希、加密工具
        │   │   │   └── util/
        │   │   ├── data/
        │   │   │   ├── db/                     # Room Database、Entity、Dao、Converters、Migrations
        │   │   │   ├── settings/               # DataStore 封装（SettingsRepository）
        │   │   │   ├── repository/             # CycleRepository、DayLogRepository 等实现
        │   │   │   └── backup/                 # 备份编解码、导入导出
        │   │   ├── domain/
        │   │   │   ├── model/                  # 纯 Kotlin 领域模型（Period、DayLog、Symptom…）
        │   │   │   ├── prediction/             # 预测引擎（纯 Kotlin，零 Android 依赖）
        │   │   │   ├── stats/                  # 统计计算（纯 Kotlin）
        │   │   │   └── usecase/                # 标记经期、编辑记录等业务规则
        │   │   ├── reminder/                   # 通知渠道、Worker、调度器
        │   │   ├── ui/
        │   │   │   ├── theme/                  # 颜色、字体、形状、主题
        │   │   │   ├── components/             # 通用组件（日历格子、环形进度、PIN 键盘…）
        │   │   │   ├── navigation/
        │   │   │   ├── onboarding/
        │   │   │   ├── today/
        │   │   │   ├── calendar/
        │   │   │   ├── daylog/                 # 每日记录编辑（Bottom Sheet）
        │   │   │   ├── stats/
        │   │   │   ├── settings/
        │   │   │   ├── lock/
        │   │   │   └── backup/
        │   │   └── di/                         # Hilt Modules
        │   └── res/
        │       ├── values/strings.xml          # 默认 = 简体中文
        │       ├── values-en/strings.xml       # 英文
        │       ├── xml/data_extraction_rules.xml
        │       ├── drawable/                   # 矢量图标
        │       └── mipmap-anydpi-v26/          # 自适应图标（含 monochrome 主题图标）
        ├── debug/       # 仅 debug 构建：填充虚构示例数据（用于截图），release 中不得存在
        ├── test/        # JVM 单元测试（预测、统计、用例、备份编解码、ViewModel）
        └── androidTest/ # 仪器测试（Room+SQLCipher、关键 UI 流程）
```

## 4. 分层与数据流

```
Compose Screen  ──事件──▶  ViewModel  ──调用──▶  UseCase / Repository  ──▶  Room(SQLCipher) / DataStore
      ▲                       │
      └──── StateFlow<UiState>┘          domain/prediction、domain/stats 为纯函数，被 ViewModel/UseCase 调用
```

规则：
- **单向数据流**：每个页面一个 `UiState`（不可变 data class），ViewModel 暴露 `StateFlow<UiState>`，UI 通过回调把事件传给 ViewModel。
- **Repository 暴露 `Flow`**：数据变化自动驱动 UI 和预测重新计算。
- **业务规则只放在 domain 层**（例如「标记某天为经期」时如何合并/拆分经期，见 `05_DATA_MODEL.md`），不要散落在 UI 或 DAO 里。
- **“今天”必须可注入**：所有用到当前日期的地方通过 `Clock`/`TodayProvider` 获取，测试中固定日期。不得在业务代码中直接调用 `LocalDate.now()`。
- **数据库实现可替换**：Room 的 `openHelperFactory` 通过 DI 提供；单元测试/Robolectric 测试使用不加密的内存数据库，仪器测试覆盖真实 SQLCipher。
- **错误处理**：Repository 层捕获异常并转为领域错误；UI 给出友好中文提示，不展示堆栈。

## 5. 构建配置要点

- `release`：`isMinifyEnabled = true`、`isShrinkResources = true`，包含 `02_PRIVACY_RED_LINES.md` 中的去日志 R8 规则，以及 SQLCipher/Room/kotlinx.serialization 需要的 keep 规则。
- 签名：从仓库根目录 `keystore.properties`（**不提交**，加入 `.gitignore`）读取；文件不存在时 release 使用 debug 签名并在构建日志中醒目警告（仅供本地测试），`scripts/release.ps1` 在这种情况下必须拒绝发布。
- `versionName` 在 `app/build.gradle.kts` 维护，从 `"0.1.0"` 开始；`versionCode` 由它自动计算（见 `11_OPEN_SOURCE_AND_RELEASE.md` §3.1）。
- `buildConfig = true`（如需要）；不得在 BuildConfig 中放任何密钥。
- 只打包 `zh`、`en` 语言资源：`androidResources { localeFilters += listOf("zh", "en") }`（或当前 AGP 的等效写法）。
- ABI：默认打全部 ABI（SQLCipher 原生库）；后续可考虑按 ABI 拆分 APK，记录在 `DECISIONS.md`。
