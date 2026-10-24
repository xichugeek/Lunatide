# 02 · 隐私红线（最高优先级，不可违反）

> 本文件中的每一条都是**硬约束**。任何功能需求、任何第三方库、任何「为了方便」的理由都不能突破。
> 不仅要遵守，还要用**自动化检查**保证以后也不会被无意破坏（见第 3 节）。

## 1. 红线清单

### R1 · 不联网
- `AndroidManifest.xml`（**合并后的最终 Manifest**）中**不得出现** `android.permission.INTERNET`、`android.permission.ACCESS_NETWORK_STATE`、`android.permission.ACCESS_WIFI_STATE`。
- 有些库（例如 WorkManager）会在自己的 Manifest 里声明 `ACCESS_NETWORK_STATE`，必须在 App 的 Manifest 中用 `tools:node="remove"` 显式移除：
  ```xml
  <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" tools:node="remove" />
  ```
- 源码中不得使用：`java.net.URL`/`HttpURLConnection`/`Socket`、OkHttp、Retrofit、Ktor、Volley、`WebView`、`DownloadManager`。
- 外部链接（例如 GitHub 主页）一律**不放**。如果确实需要（例如「关于」页的开源地址），只以**纯文本**显示，可点击「复制」，不调用浏览器打开。

### R2 · 无账号、无标识符
- 不读取、不生成任何设备/用户标识：IMEI、Android ID、广告 ID（GAID/OAID）、MAC、序列号、手机号。
- 不申请 `READ_PHONE_STATE`、`READ_CONTACTS`、定位、相机、麦克风、日历、短信、存储（`READ/WRITE_EXTERNAL_STORAGE`、`MANAGE_EXTERNAL_STORAGE`、`READ_MEDIA_*`）等权限。
- 文件导入导出**只用 SAF**（`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`），不需要存储权限。

### R3 · 无第三方 SDK 收集数据
- 禁止任何统计、埋点、崩溃上报、广告、推送、热更新、A/B 测试 SDK。黑名单（不限于）：Firebase 全家桶、Google Play Services（`com.google.android.gms`）、Crashlytics、友盟（`com.umeng`）、Bugly/腾讯系（`com.tencent`）、百度（`com.baidu`）、字节/穿山甲（`com.bytedance`、`com.pangle`）、Facebook SDK、Sentry、AppsFlyer、Adjust、个推、极光（`cn.jiguang`、`cn.jpush`）、小米/华为/OPPO/vivo 推送 SDK。
- 允许的依赖只有 `03_TECH_ARCHITECTURE.md` 的白名单。

### R4 · 数据只在本机，且加密存储
- 所有用户健康数据存放在 App 私有目录的 **SQLCipher 加密数据库**中（细节见 `08_SECURITY_AND_BACKUP.md`）。
- 数据库密钥由 Android Keystore 保护，**不得硬编码**，不得写入日志。
- 禁止把任何健康数据写到：外部存储、公共媒体目录、剪贴板（用户主动点「复制」除外）、系统日历、系统健康平台（Health Connect）。

### R5 · 禁止系统自动备份把数据带出手机
- `android:allowBackup="false"`
- `android:fullBackupContent="false"`（兼容 Android 11 及以下）
- `android:dataExtractionRules="@xml/data_extraction_rules"`（Android 12+），在 `cloud-backup` 和 `device-transfer` 中都 `exclude` 全部域（`root`、`file`、`database`、`sharedpref`、`external`）。
- 说明：因此卸载 App 或换机时数据不会自动保留，**这是预期行为**；用户靠「加密备份导出/导入」迁移数据。App 内必须清楚告知这一点。

### R6 · 日志不含隐私
- 不得用 `Log.*`/`println` 输出任何用户数据（日期、症状、备注、PIN、密钥等）。
- release 构建通过 R8 规则去掉日志调用：
  ```
  -assumenosideeffects class android.util.Log {
      public static *** v(...);
      public static *** d(...);
      public static *** i(...);
      public static *** w(...);
      public static *** e(...);
  }
  ```
- 崩溃时不上报。可选：本地保留最近一次崩溃的**堆栈**（不含用户数据），用户可在「关于」页查看并自行复制给开发者。

### R7 · 通知不泄露隐私
- 所有通知设置 `NotificationCompat.VISIBILITY_PRIVATE`，并提供 `publicVersion`（锁屏上只显示「月汐 · 你有一条提醒」）。
- 提供「通知隐私模式」开关（**默认开启**）：开启时通知正文也只显示中性文案，不出现「经期」「例假」「排卵」「易孕」等字样。

### R8 · 屏幕内容保护
- 提供「防截屏/最近任务隐藏」开关（`FLAG_SECURE`）。开启应用锁时默认开启，用户可单独关闭（有人需要截图给医生看）。

### R9 · 输入法隐私
- 备注等自由文本输入框：关闭自动纠错/联想；如果技术上可行，设置 `EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING`（Compose 不直接支持时可用 `AndroidView` 包装 `EditText`，或使用 Compose 中等效的平台选项；实现方式记录在 `DECISIONS.md`）。
- PIN 输入只用 App 自绘的数字键盘，**不调用系统输入法**。

### R10 · 诚实的隐私说明
- App 内「隐私政策」页面（纯本地文本，不是网页）和仓库根目录 `PRIVACY.md` 内容一致，清楚说明：收集什么（只在本机）、不收集什么、数据在哪、怎么备份、怎么删除、卸载即删除。
- 不得宣称做不到的事。例如：应用锁 PIN 是界面层保护，数据库加密靠的是系统 Keystore；Root 过的手机、已解锁的手机被他人拿到时，保护强度有限——在隐私政策里如实写明。

## 2. 允许的权限白名单（合并后 Manifest 中只能出现这些）

| 权限 | 来源/用途 |
|---|---|
| `android.permission.POST_NOTIFICATIONS` | Android 13+ 发提醒通知（运行时在合适时机申请） |
| `android.permission.USE_BIOMETRIC` | 指纹/面容解锁（androidx.biometric 自动添加） |
| `android.permission.USE_FINGERPRINT` | 同上，旧版本兼容（androidx.biometric 自动添加） |
| `android.permission.RECEIVE_BOOT_COMPLETED` | 开机后恢复提醒调度（WorkManager 自动添加） |
| `android.permission.WAKE_LOCK` | WorkManager 自动添加 |
| `android.permission.FOREGROUND_SERVICE` | 若 WorkManager 版本自动添加则允许；App 自身不使用前台服务 |
| `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AGP/AndroidX 自动生成的内部权限 |

出现任何不在白名单中的权限 → 构建失败。若某个新版本库引入了新权限，先评估：能移除就用 `tools:node="remove"` 移除；确实必须保留的，写进 `DECISIONS.md` 并更新检查脚本白名单（同时更新本文件建议，见总规则第 5 条）。

## 3. 自动化检查（M0 里程碑必须完成，并接入 CI）

在 `app/build.gradle.kts`（或 `buildSrc`/`build-logic`）中实现 Gradle 任务 **`verifyPrivacy`**，并让 `check` 任务依赖它。它包含三项检查，任一失败则构建失败，并输出清晰的中文错误说明：

### 3.1 `verifyManifestPermissions`
- 读取 `debug` 和 `release` 两个变体**合并后的 Manifest**（`processXxxMainManifest` / `processXxxManifest` 任务的输出，可通过 AGP 的 `artifacts`/`SingleArtifact.MERGED_MANIFEST` API 获取）。
- 解析所有 `<uses-permission>` 与 `<uses-permission-sdk-23>`，与第 2 节白名单比对。
- 同时断言 `<application>` 上 `allowBackup=false`，且不存在 `usesCleartextTraffic=true`。

### 3.2 `verifyDependencies`
- 遍历 `releaseRuntimeClasspath` 的所有已解析依赖（含传递依赖），任何 group 以黑名单前缀开头的即失败。黑名单至少包含：
  `com.google.firebase`, `com.google.android.gms`, `com.google.android.play`, `com.crashlytics`, `com.squareup.okhttp`, `com.squareup.okhttp3`, `com.squareup.retrofit2`, `io.ktor`, `com.android.volley`, `com.umeng`, `com.tencent`, `com.baidu`, `com.bytedance`, `com.pangle`, `com.facebook`, `io.sentry`, `com.appsflyer`, `com.adjust`, `cn.jiguang`, `cn.jpush`, `com.getui`, `com.igexin`, `com.huawei.hms`, `com.xiaomi`, `com.heytap`, `com.vivo`
- 把最终依赖清单（group:artifact:version）写到 `app/build/reports/privacy/dependencies.txt`，方便人工审查。

### 3.3 `verifySourceForbiddenApis`
- 扫描 `app/src/main/**/*.kt` 与 `*.java`，出现以下字符串即失败（注释中出现也算，避免歧义）：
  `java.net.`, `HttpURLConnection`, `okhttp`, `retrofit`, `io.ktor`, `WebView`, `DownloadManager`, `Settings.Secure.ANDROID_ID`, `getDeviceId`, `AdvertisingIdClient`, `getImei`
- 允许通过 `app/privacy-allowlist.txt` 对**个别文件**加豁免，但每条豁免都必须在 `DECISIONS.md` 说明理由（预期 v1.0 豁免数为 0）。

### 3.4 CI
- `.github/workflows/android.yml`：在 push 和 PR 时运行 `./gradlew verifyPrivacy testDebugUnitTest lintDebug assembleDebug`。
- CI 本身联网下载依赖没有问题——红线约束的是 **App**，不是构建环境。

## 4. 每次提交前的自查（写进 AGENTS.md）

- [ ] 我加了新依赖吗？它在白名单里吗？它会引入新权限吗？
- [ ] 我有没有把用户数据写进日志、文件、剪贴板或通知？
- [ ] `verifyPrivacy` 通过了吗？
