# 月汐 Lunatide

完全离线的经期记录 Android App，所有健康数据在本机加密保存。

开始前完整阅读 `Agent/README.md` 并按其顺序读取 01～11。进度和决策分别记入 `Agent/PROGRESS.md`、`Agent/DECISIONS.md`；不得修改其他规范文件。

## 硬约束
- `Agent/02_PRIVACY_RED_LINES.md` 最高优先级；最终 Manifest 绝不含网络权限。
- 无账号、广告、遥测、第三方收集数据 SDK；依赖仅用白名单。
- 不提交密钥、个人信息、真实记录、备份或 APK。截图仅用虚构数据。
- 规范不明确时选更保护隐私、更简单的方案，记录决策后继续。
- 按 M0～M10 顺序实现，每个里程碑验证、更新进度、提交、推送。

## 命令
`./gradlew verifyPrivacy testDebugUnitTest lintDebug assembleDebug`

Windows 用 `.\gradlew.bat`；有设备时运行 `connectedDebugAndroidTest`。

## 提交前自查
- 我加了新依赖吗？它在白名单里吗？它会引入新权限吗？
- 我有没有把用户数据写进日志、文件、剪贴板或通知？
- `verifyPrivacy` 通过了吗？
- 测试没有运行时如实标记未验证，不能宣称通过。
