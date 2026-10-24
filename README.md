# 月汐 Lunatide

不注册、不联网、无广告，只属于你的周期记录。

月汐是一款免费、开源的 Android 经期记录 App，当前正在开发。

- 无需账号，没有广告和社区。
- App 不申请网络权限，健康数据仅在本机保存。
- 计划使用 SQLCipher 加密数据库、Android Keystore 保护密钥，以及用户主动导出的加密备份。

## 下载与截图

尚未发布可安装的正式版本。开发完成后通过 GitHub Releases 和官网提供下载。
截图待开发与设备验证后补充，所有演示数据均为虚构。

## 开发范围

周期记录、日历与预测、每日感受、统计、离线提醒、应用锁和加密备份，按 `Agent/09_DEV_WORKFLOW_AND_MILESTONES.md` 推进。预测仅供参考，不能用于避孕或代替诊断。

## 从源码构建

安装 Android Studio、JDK 17 和 Android SDK API 37（当前使用稳定 minor API 2）。

```powershell
git clone https://github.com/xichugeek/Lunatide.git
cd Lunatide
.\gradlew.bat verifyPrivacy testDebugUnitTest lintDebug assembleDebug
```

SDK 路径写入不提交的 `local.properties`；不要提交签名密钥。未配置正式签名时，release 仅使用 debug 签名作本地测试，不能发布。

## APK 校验

正式发布时将公布 APK SHA-256 和签名证书 SHA-256 指纹。当前没有正式 APK 或证书指纹可供校验。

## 隐私与许可

参阅 [隐私政策](PRIVACY.md)、[安全说明](SECURITY.md) 和 [MIT 许可证](LICENSE)。
