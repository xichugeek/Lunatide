# 11 · 开源与发布

> 已确定：**开源（MIT）**，通过 **GitHub Releases** 和 **西楚极客官网项目页**发布 APK。暂不上架应用商店。
> 官网项目页参照现有的「西楚记账」：https://xichugeek.com/projects/xichu-finance/
> 官网本身由单独的流程发布，不在本仓库内修改。你（Codex）负责的是**仓库内容、可安装的 APK、校验信息和官网素材包**。

## 1. 许可证与仓库文件

### 1.1 许可证
- `LICENSE`：MIT，版权行 `Copyright (c) 2026 西楚极客 XichuGeek`。**M0 就创建。**
- `THIRD_PARTY_NOTICES.md`：列出所有运行时依赖（库名、版本、许可证、项目主页的纯文本地址），内容与 App 内「开源许可」页面一致。SQLCipher Community Edition 的许可证全文必须包含在内（它要求在文档中保留版权声明）。

### 1.2 根目录文件清单

| 文件 | 内容 | 创建时机 |
|---|---|---|
| `README.md` | 中文为主。顶部：图标 + 「月汐 Lunatide」+ 一句话介绍 + 隐私三承诺；然后依次是：下载（官网、GitHub Releases）、截图、功能、**如何校验 APK**（SHA-256 和签名证书指纹）、从源码构建、隐私说明链接、许可证 | M0 初稿，M10 完善 |
| `PRIVACY.md` | 隐私政策（与 App 内一致） | M0 初稿，M9 定稿 |
| `SECURITY.md` | 安全模型（防护哪些风险、不防护哪些风险，如实说明）+ 如何报告安全问题（GitHub Security Advisories 私密报告） | M0 初稿，M10 定稿 |
| `CHANGELOG.md` | Keep a Changelog 格式，中文 | M0 起，每个版本更新 |
| `THIRD_PARTY_NOTICES.md` | 见 §1.1 | M9 |
| `AGENTS.md` | 见 09 文档 M0 | M0 |
| `docs/BUILD_FROM_SOURCE.md` | **新手指南**：安装 Android Studio → 克隆仓库 → 打开项目 → 运行到手机/模拟器 → 构建 APK。截图可选，步骤要具体到菜单名 | M10 |
| `docs/ANDROID_RELEASE.md` | **发布指南**：生成签名密钥、配置 `keystore.properties`、运行发布脚本、创建 GitHub Release、更新官网素材 | M10 |
| `docs/site/` | 官网素材包，见 §5 | M10 |
| `.github/ISSUE_TEMPLATE/bug_report.md` | 问题模板，**醒目提示**：「请不要在问题里粘贴你的真实经期记录、备份文件或含真实数据的截图」 | M0 |
| `.github/ISSUE_TEMPLATE/feature_request.md` | 功能建议模板，并说明「月汐不会加入联网、账号、广告、社区类功能」 | M0 |

### 1.3 开源仓库卫生
- 不提交：签名密钥、`keystore.properties`、`local.properties`、真实用户数据、备份文件、APK（APK 只放在 GitHub Releases）。
- 截图、示例数据一律**虚构**，并在使用处注明「截图为虚构示例数据」。
- 提交前检查不含个人信息（真实姓名、手机号、本机路径中的用户名等）。

## 2. 发布渠道与更新方式

| 渠道 | 内容 |
|---|---|
| GitHub Releases | Tag `vX.Y.Z`；附件：`Lunatide-vX.Y.Z.apk`、`Lunatide-vX.Y.Z.apk.sha256`；正文 = 该版本 CHANGELOG + 校验信息 + 安装/升级说明 |
| 官网项目页 | 计划地址 `https://xichugeek.com/projects/lunatide/`（最终 slug 以官网为准）；内容来自 `docs/site/` |

- **App 不检查更新**（不联网）。「关于」页显示当前版本号，以及一行说明：「月汐不会联网检查更新。新版本请访问官网或 GitHub 查看」，下面以**纯文本**显示两个地址，各带「复制」按钮（遵守 R1，不调用浏览器）。
  - 官网：`https://xichugeek.com/projects/lunatide/`
  - GitHub：`https://github.com/xichugeek/Lunatide/releases`

## 3. 版本号与签名

### 3.1 版本号
- 语义化版本 `MAJOR.MINOR.PATCH`。开发阶段 `0.x.y`，首个正式版 `1.0.0`。
- `versionCode = MAJOR × 10000 + MINOR × 100 + PATCH`（例如 1.0.0 → 10000，1.2.3 → 10203），在 `app/build.gradle.kts` 中由 `versionName` 计算，避免手工维护出错。

### 3.2 签名密钥（由人类生成和保管）
- 签名密钥**由人类生成**。M10 时，你把下面的命令（填好建议的参数）交给人类执行，**不要**自己生成或保存密码：
  ```powershell
  keytool -genkeypair -v -keystore D:\Project\Android\keystores\lunatide-release.jks `
    -alias lunatide -keyalg RSA -keysize 4096 -validity 36500
  ```
- 仓库根目录 `keystore.properties`（已在 `.gitignore`，由人类填写）：
  ```properties
  storeFile=D:/Project/Android/keystores/lunatide-release.jks
  storePassword=...
  keyAlias=lunatide
  keyPassword=...
  ```
- 在 `docs/ANDROID_RELEASE.md` 中用醒目方式写明：
  - 密钥和密码至少备份在两个不同的地方。
  - **密钥一旦丢失，已安装的用户无法覆盖升级**；而月汐的数据只在手机本地，用户只能先导出备份、卸载、安装新签名版本、再导入。所以**永远不要更换签名密钥**。
- 签名方案使用 AGP 默认（v2/v3 签名）。

### 3.3 公开校验信息
每个版本公布两项，用户可以自行核对安装包是否被篡改：
1. APK 文件的 **SHA-256**。
2. 签名证书的 **SHA-256 指纹**（`apksigner verify --print-certs` 输出中的 `Signer #1 certificate SHA-256 digest`）。该值在所有版本中应保持不变，写进 README、Release 正文和官网。

## 4. 发布脚本 `scripts/release.ps1`（M10 创建）

在 Windows PowerShell 中运行：`.\scripts\release.ps1`。依次执行，任一步失败即停止并输出中文原因：

1. 检查 Git 工作区干净、当前在 `main`、`keystore.properties` 存在。
2. 读取 `versionName`/`versionCode`；检查 `CHANGELOG.md` 中有该版本的条目。
3. 运行 `verifyPrivacy testDebugUnitTest lintRelease assembleRelease`。
4. 复制 APK 到 `dist/Lunatide-vX.Y.Z.apk`（`dist/` 在 `.gitignore` 中）。
5. 用 Android SDK `build-tools` 中的 `apksigner verify --print-certs` 校验签名，提取证书 SHA-256。
6. 用 `apkanalyzer manifest permissions`（或 `aapt2 dump permissions`）读取**最终 APK** 的权限，再次与 `02_PRIVACY_RED_LINES.md` §2 白名单比对。
7. 计算 APK 的 SHA-256（`Get-FileHash -Algorithm SHA256`），写入 `dist/Lunatide-vX.Y.Z.apk.sha256`，格式与 `sha256sum` 兼容：`<小写hash>  Lunatide-vX.Y.Z.apk`。
8. 写 `dist/release-info.json`：
   ```json
   {
     "app": "Lunatide", "versionName": "1.0.0", "versionCode": 10000,
     "apkFile": "Lunatide-vX.Y.Z.apk", "apkBytes": 0, "apkSha256": "...",
     "signerCertSha256": "...", "minSdk": 26, "targetSdk": 0,
     "permissions": ["..."], "gitCommit": "...", "builtAt": "ISO-8601 +08:00"
   }
   ```
9. 生成 `dist/RELEASE_NOTES-vX.Y.Z.md`（CHANGELOG 该版本内容 + 校验信息 + 安装/升级说明），作为 GitHub Release 正文。
10. 最后打印下一步需要人类确认的命令（**脚本本身不推送 tag、不创建 Release**）：
    ```powershell
    git tag vX.Y.Z; git push origin vX.Y.Z
    gh release create vX.Y.Z dist\Lunatide-vX.Y.Z.apk dist\Lunatide-vX.Y.Z.apk.sha256 `
      --title "月汐 vX.Y.Z" --notes-file dist\RELEASE_NOTES-vX.Y.Z.md
    ```
    推送 tag 和创建 Release 需要先在汇报中说明并征得人类同意。

## 5. 官网素材包 `docs/site/`（M10 生成，每次发布更新）

目标：网站一方拿到这个目录就能直接做出和「西楚记账」同样结构的项目页，不需要再向你要任何信息。

```
docs/site/
├── project-page.md          # 页面全部文案，按下方章节组织
├── release-info.json        # 从 dist/ 复制
├── icon-512.png             # 应用图标 512×512
└── screenshots/
    ├── 01-today.png         # 今天页（经期中或等待下次状态）
    ├── 02-calendar.png      # 日历（同时能看到已记录经期、预测经期、易孕期）
    ├── 03-daylog.png        # 每日记录编辑
    ├── 04-stats.png         # 统计页（有柱状图）
    ├── 05-privacy.png       # 隐私与安全设置
    └── captions.json        # 每张图的中英文标题，例如 {"01-today.png": {"zh": "今天", "en": "Today"}}
```

### 5.1 `project-page.md` 的章节（对应西楚记账页面结构）

1. **标题块**：`PROJECT / 00X`（编号留给网站一方填写）· 标题「月汐」· 副标题「Lunatide · Private Period Tracker」· 标签：`Android`、`Open Source`、`APK vX.Y.Z`、`Offline · No Network`
2. **项目简介**：2–3 段。痛点（广告、社区骚扰、强制注册、隐私担忧）→ 月汐的做法 → 适合谁。
3. **实际截图**：引用 `screenshots/`，注明「Android 模拟器运行，截图为虚构示例数据」。
4. **下载与正式版本**：版本号、APK 文件名、字节数、SHA-256、签名证书指纹；链接：下载 APK（GitHub Release 附件地址）、Release Notes、SHA256 文件、GitHub 仓库。
5. **核心能力**：6–8 条要点（一键记录、日历与预测、每日记录、统计、提醒、应用锁、加密备份）。
6. **隐私设计**：没有网络权限（附「如何自己验证」：系统设置里查看权限）；不注册；SQLCipher 加密数据库；禁止系统云备份；通知隐私模式；不含任何第三方 SDK；开源可审查。
7. **项目架构**：`Compose UI → ViewModel → Repository → Room + SQLCipher（本机）`，以及「没有服务器」的说明；可附一张简单文字架构图。
8. **安装与升级**：需要 Android 8.0 及以上；允许「安装未知来源应用」；**升级时直接覆盖安装，不要先卸载**；卸载会删除全部数据，卸载或换机前先在「设置 → 数据」导出加密备份；App 不会自动检查更新。
9. **从源码到上线**：4 步（下载 APK 或克隆源码 → 用 Android Studio 打开 → 运行/构建 → 自行签名），链接 `docs/BUILD_FROM_SOURCE.md` 和 `docs/ANDROID_RELEASE.md`。
10. **当前范围与限制**（如实写）：没有云同步，换机需手动备份；PIN 是界面锁，数据库加密依赖系统 Keystore；已 Root 或已被他人解锁的手机保护有限；预测仅供参考，不能用于避孕；部分国产系统可能延迟或拦截提醒；目前只有中文和英文。
11. **技术栈**：Kotlin、Jetpack Compose、Material 3、Room、SQLCipher、DataStore、WorkManager、Hilt。
12. **项目链接**：GitHub 仓库、Releases、隐私政策（`PRIVACY.md`）、安全说明（`SECURITY.md`）、新手指南、发布指南。

文案风格遵守 `07_UI_UX_GUIDE.md` §6：平实、具体、不夸大。所有数字（体积、SHA-256、版本）都来自 `release-info.json`，不要手填。

### 5.2 截图怎么做
- 在 **debug 源集**（`app/src/debug/`）中提供「填充虚构示例数据」的入口（例如 debug 构建下设置页底部的「开发者：填充示例数据」）。这段代码**只能**存在于 `src/debug/`，release 构建中不得包含；在 `verifyPrivacy` 或单元测试中加一项检查确认 release 不含该入口。
- 示例数据：约 8 个周期（周期 27–31 天，经期 5–6 天），带一些症状、心情；没有同房记录，备注用中性内容（如「多喝热水」「早点睡」）。
- 用模拟器（建议 1080×2400、简体中文、浅色模式）截图：`adb exec-out screencap -p > docs/site/screenshots/01-today.png`。截图前把系统时间调到示例数据合适的日期，状态栏尽量干净（可使用系统演示模式）。
- 没有可用模拟器时：截图标记为待办，写进 `PROGRESS.md`，在汇报中告诉人类，不要用其他方式伪造截图。

## 6. 每次发版清单

- [ ] `versionName` 已更新，`CHANGELOG.md` 有该版本条目
- [ ] `.\scripts\release.ps1` 全部通过
- [ ] 签名证书指纹与上一版本一致
- [ ] `docs/site/` 已更新（`release-info.json`、必要时更新截图和文案）
- [ ] README 中的版本号、校验信息已更新
- [ ] 人类同意后：推送 tag、创建 GitHub Release、上传 APK 和 `.sha256`
- [ ] 通知人类可以更新官网项目页
