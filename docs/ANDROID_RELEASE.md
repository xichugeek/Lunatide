# Android 发布指南

发布渠道为 GitHub Releases 与官网项目页。App 不联网检查更新。官网由独立流程发布，本仓库提供 APK 校验信息和 `docs/site/` 素材，不修改官网服务。

脚本需要 PowerShell 7 或以上（`pwsh`）；旧版 Windows PowerShell 5.1 不支持脚本使用的无 BOM UTF-8 写入选项。

## 1. 由维护者生成并保管签名密钥

**这一步必须由人类执行。不要把密码发给开发代理，不要提交密钥或 `keystore.properties`。**

在本机终端创建密钥目录后执行（`keytool` 位于 JDK/JBR 的 `bin` 中）：

```powershell
New-Item -ItemType Directory -Force D:\Project\Android\keystores
keytool -genkeypair -v -keystore D:\Project\Android\keystores\lunatide-release.jks `
  -alias lunatide -keyalg RSA -keysize 4096 -validity 36500
```

按提示输入密码。证书主体使用项目/组织信息，不填写个人真实姓名、住址或手机号：例如 CN=Lunatide、OU=Open Source、O=XichuGeek，其余可填 Unknown。证书主体会包含在公开 APK 中。

在仓库根目录由人类创建 `keystore.properties`（已在 `.gitignore`）：

```properties
storeFile=D:/Project/Android/keystores/lunatide-release.jks
storePassword=在本机填写密码
keyAlias=lunatide
keyPassword=在本机填写密码
```

Java Properties 中反斜杠具有转义含义；路径使用 `/`，密码含反斜杠时按 Properties 规则转义。不要把该文件内容贴到聊天或 Issue 中。

**密钥和密码至少备份在两个不同的地方。永远不要更换正式签名密钥。** 密钥丢失后已安装用户无法覆盖升级，只能先导出加密备份，再卸载、安装新签名版本并导入；卸载前未导出会丢失本机记录。

## 2. 核对版本与构建

更新 `app/build.gradle.kts` 的 `versionName`；`versionCode = MAJOR × 10000 + MINOR × 100 + PATCH` 自动计算。首版为 1.0.0 / 10000。`CHANGELOG.md` 必须有对应条目。

确认在 `main`，工作区干净，依赖与文档无秘密、个人信息或真实记录。运行：

```powershell
.\scripts\release.ps1
```

脚本检查 Git、正式签名配置、版本和更新日志，再运行 `verifyPrivacy testDebugUnitTest lintRelease assembleRelease`。AGP 默认签名方案保留；R8、资源压缩和去日志规则在 release 生效。

脚本验证签名、拒绝 Android Debug 证书、检查最终 APK 权限、SDK/版本及示例入口隔离，然后生成：

- `dist/Lunatide-vX.Y.Z.apk`
- `dist/Lunatide-vX.Y.Z.apk.sha256`
- `dist/release-info.json`
- `dist/RELEASE_NOTES-vX.Y.Z.md`

`dist/` 被 Git 忽略；APK 仅作为 GitHub Release 附件分发。脚本不创建或推送标签，不创建 Release。

用另一遍哈希计算核对 `.sha256`，例如：

```powershell
$expected = (Get-Content dist/Lunatide-v1.0.0.apk.sha256).Split(' ')[0]
$actual = (Get-FileHash dist/Lunatide-v1.0.0.apk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($expected -ne $actual) { throw 'APK 校验不一致' }
```

debug 测试包与正式包的证书不同，不能直接覆盖安装；如已使用测试包，请先导出备份，再由用户自行卸载测试包、安装正式包并导入，不能自动删除用户数据。

每次升级还要核对证书 SHA-256 与上一正式版本一致，并实际覆盖安装检查数据保留。不要只比较文件哈希：每次构建或重新签名后 APK 哈希可能变化。

## 3. 更新截图与官网素材

debug 构建的设置页底部可填充约 8 个虚构周期，确认后会替换本机记录；release 没有此入口。截图前设为简体中文、浅色、100% 字号、1080×2400，关闭不必要的通知。可临时将模拟器日期设为当月 12 日，再填充示例，使同一月视图能看见记录、预测和易孕期；完成后恢复设备时间。

`docs/site/screenshots/` 的 5 张图必须来自实际运行，标题在 `captions.json`，所有使用处注明虚构数据。图标由仓库矢量绘制，`scripts/render-icon.ps1` 可重生成 512px PNG。

正式签名脚本成功后运行：

```powershell
.\scripts\update-site.ps1
```

它只从真实 `dist/release-info.json` 和 APK 更新官网文案、公开校验信息与 README，不接受手填假哈希。检查并提交这些文档。`gitCommit` 记录实际构建代码的提交；随后仅更新素材/元数据的提交不会改变已构建 APK。

## 4. 等人类同意后公开发布

先汇报版本、APK 大小、权限、SHA-256、证书指纹和验收中的未验证项目。**获得人类明确同意后**，才执行脚本打印的 `git tag`、`git push origin vX.Y.Z` 和 `gh release create` 命令。

发布后检查附件可下载、哈希与签名指纹一致，再通知官网维护流程使用 `docs/site/`。不要自动部署网站或加入联网更新 SDK。
