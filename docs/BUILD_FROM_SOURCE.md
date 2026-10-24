# 从源码构建月汐

月汐是完全离线的 Android App。构建工具需要下载公开依赖，App 本身没有网络权限。无需部署服务器或填写 API 密钥。

## 1. 安装开发工具

1. 从 developer.android.com/studio 安装当前稳定 Android Studio，首次启动按 Standard 完成向导。
2. 打开 **More Actions → SDK Manager**（已打开项目时为 **Tools → SDK Manager**）。在 SDK Platforms 安装 **Android SDK Platform 37.2**；在 SDK Tools 安装 **Android SDK Build-Tools 36.0.0**、**Android SDK Platform-Tools**、**Android Emulator**、**Android SDK Command-line Tools (latest)**。
3. 记录 SDK Location。Windows 默认常在用户目录下，不要把本机用户名路径写进仓库。
4. 安装 JDK 17（用于 Kotlin `jvmToolchain(17)`）。Gradle 运行使用 Android Studio 自带 JBR 21 或 JDK 21。Studio 中可在 **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** 选择内置 JBR。
5. 安装 Git。使用 Windows 终端执行脚本时推荐 PowerShell 7。

## 2. 下载并打开项目

```powershell
git clone https://github.com/xichugeek/Lunatide.git
cd Lunatide
```

在 Android Studio 选择 **Open**，选中仓库根目录，等待 Gradle Sync 完成。首次同步需要下载 Gradle 与依赖；不要手工复制 jar，也不要通过旧工具链绕过错误。

若提示找不到 SDK，在根目录新建 `local.properties`，例如：

```properties
sdk.dir=D\:/Android/Sdk
```

也可以设置 `ANDROID_HOME`。`local.properties` 已忽略，绝不提交。若命令行找不到 Java，设置 `JAVA_HOME` 为 JBR/JDK 21 安装目录，再重新打开终端。JDK 17 可在 Studio 下载或放入 Gradle 可发现的 JDK 目录；检查 `./gradlew -q javaToolchains`。

## 3. 运行到模拟器或手机

1. **Tools → Device Manager → Create Virtual Device**，选择 Pixel 7 等设备，下载稳定系统镜像并启动。项目最低支持 Android 8.0（API 26）；本仓库已在 API 35 模拟器运行，但不代表所有系统/厂商都已验证。
2. 或在手机开启开发者选项与 USB 调试，连接数据线，确认调试授权。
3. Studio 顶部选择 `app`、目标设备，点绿色 **Run**。首次安装无需账号；中文界面可在支持的系统应用语言设置中选择简体中文。
4. 使用虚构记录测试。不要把真实记录、备份、密钥或含真实数据的截图发到公开 Issue。

## 4. 构建与验证

```powershell
.\gradlew.bat verifyPrivacy testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest  # 需要已连接设备
```

Linux/macOS 改为 `./gradlew`，必要时先执行 `chmod +x gradlew`。debug APK 在 `app/build/outputs/apk/debug/app-debug.apk`，仅作开发测试；不要将 APK 提交 Git。

`verifyPrivacy` 检查最终 debug/release Manifest、依赖、禁用 API、系统备份配置和示例入口隔离。依赖表在 `app/build/reports/privacy/dependencies.txt`；Lint 和测试报告在 `app/build/reports/`。

正式发布请阅读 [发布指南](ANDROID_RELEASE.md)。没有正式签名时 `assembleRelease` 会明确警告并使用 debug 证书供本地验证，发布脚本会拒绝此类包。
