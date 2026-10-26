<img src="docs/site/icon-512.png" width="80" alt="月汐图标">

# 月汐 Lunatide

不注册、不联网、无广告，只属于你的周期记录。

月汐是一款免费、开源的 Android 经期记录 App。安静地记下自己的节奏，数据由你保管。

- 无需账号，没有广告和社区。
- 没有网络权限，不上传记录或使用行为。
- SQLCipher 加密保存健康记录，Keystore 保护密钥；迁移由你主动导出加密备份。

## 下载

正式 APK 通过 [GitHub Releases](https://github.com/xichugeek/Lunatide/releases) 分发。[官网项目页](https://xichugeek.com/projects/lunatide/)为计划发布地址，由独立流程上线；请以 Releases 中实际发布的条目为准。

<!-- RELEASE_INFO_START -->
已签名构建：v1.0.0（10000），10118356 字节。

- APK：[Lunatide-v1.0.0.apk](https://github.com/xichugeek/Lunatide/releases/download/v1.0.0/Lunatide-v1.0.0.apk)
- APK SHA-256：`77e0902292ec9304619b0f31d9cf492a50e4556675c44258b5076d4e5414a39b`
- 签名证书 SHA-256：`4d4957412cedc56e24be8fec7061a1f83e9e1061cf3a97dea5965f0231a10676`

此版本已正式发布：[Release Notes](https://github.com/xichugeek/Lunatide/releases/tag/v1.0.0)。
<!-- RELEASE_INFO_END -->

需要 Android 8.0 或以上。升级时直接覆盖安装，不要先卸载；卸载或换机前先导出加密备份。月汐不会联网检查更新。

## 截图

**Android 模拟器实际运行；所有截图均为虚构示例数据。**

| 今天 | 日历 | 每日记录 |
|---|---|---|
| ![今天，虚构示例数据](docs/site/screenshots/01-today.png) | ![日历，虚构示例数据](docs/site/screenshots/02-calendar.png) | ![每日记录，虚构示例数据](docs/site/screenshots/03-daylog.png) |

| 统计 | 隐私与安全 |
|---|---|
| ![统计，虚构示例数据](docs/site/screenshots/04-stats.png) | ![隐私与安全，虚构示例数据](docs/site/screenshots/05-privacy.png) |

## 功能

- 一键记录开始和结束，支持撤销、改日期与点滴出血。
- 日历查看已记录经期和预测；预测仅供参考，不能用于避孕或代替医生诊断。
- 自动保存经量、痛经、症状、心情、体温与备注；同房记录默认隐藏。
- 周期/经期图表、症状统计与历史。
- 五种本地提醒，通知默认使用中性文案，锁屏公开版本保持中性。
- 独立 PIN、可选生物识别、防截屏；PIN 是界面锁，数据库加密由系统 Keystore 保护。
- 密码加密备份、覆盖/合并恢复、带明文警告的 CSV 导出、本机数据清除。
- 中文与英文、浅色与深色、一周起始日及记录偏好。

## 如何校验 APK

从同一正式 Release 下载 APK 与 `.sha256` 文件。在 PowerShell 中计算：

```powershell
$expected = (Get-Content .\Lunatide-v1.0.0.apk.sha256).Split(' ')[0]
$actual = (Get-FileHash .\Lunatide-v1.0.0.apk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($expected -ne $actual) { throw 'APK 文件校验不一致' }
```

Linux 可用 `sha256sum -c Lunatide-v1.0.0.apk.sha256`。还应使用 Android SDK 的 `apksigner verify --print-certs` 核对证书 SHA-256 指纹与正式发布信息一致；同一项目的正式升级证书应保持不变。SHA-256 比对能检测文件变化，也应确认下载来源可信。

## 从源码构建

详见[新手指南](docs/BUILD_FROM_SOURCE.md)与[发布指南](docs/ANDROID_RELEASE.md)。使用当前锁定工具链：Gradle 9.8.2、AGP 9.4.1、Kotlin 2.4.21、SDK Platform 37.2；JBR/JDK 21 运行 Gradle，JDK 17 编译源码。

```powershell
git clone https://github.com/xichugeek/Lunatide.git
cd Lunatide
.\gradlew.bat verifyPrivacy testDebugUnitTest lintDebug assembleDebug
```

构建环境可以联网下载依赖；App 没有网络权限。SDK 路径与签名配置仅保存在 Git 忽略的本机文件中。未配置正式签名时 release 仅使用 debug 签名作本地验证，发布脚本会拒绝分发。

## 隐私与许可

系统自动备份与设备迁移已禁用，卸载会删除本机数据。备份密码无法找回；导出的 CSV 未加密。已 Root、系统被攻破、恶意输入法或他人取得已解锁手机时，保护能力有限。

参阅[隐私政策](PRIVACY.md)、[安全说明](SECURITY.md)、[MIT 许可证](LICENSE)和[开源许可清单](THIRD_PARTY_NOTICES.md)。实际验证与未验证项目见 [PROGRESS](Agent/PROGRESS.md)；请勿在公开 Issue 上传真实健康记录或备份。
