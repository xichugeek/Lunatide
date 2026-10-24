# 开发进度（由 Codex 维护）

> 每个里程碑结束时更新。最新的放在最上面。不要删除历史记录。

## 当前状态

- 当前里程碑：M8 已完成，准备 M9 设置、打磨与国际化
- 最近一次构建：四项检查成功（2026-10-24）；累计 127 个 JVM 测试、14 个仪器测试通过；Lint 0 error、原有 1 warning
- 阻塞问题：无；原 SDK 缺失已自行修复

## 环境

| 项 | 结果 |
|---|---|
| 操作系统 | Windows NT 10.0.26100.0；PowerShell 7.6.5；检查日期 2026-10-24（Asia/Shanghai） |
| JDK | Gradle 运行于 Android Studio JBR 21.0.9；另安装 Temurin JDK 17.0.20.1，`jvmToolchain(17)` 已通过真实编译与 JVM 测试验证。 |
| Android SDK 路径 / 已安装平台 | SDK 为 `D:\Android\Sdk`；Platform 36.1、37.2；Build Tools 36.0.0、36.1.0、37.0.0；Command-line Tools 23.0。已创建被 Git 忽略的 `local.properties`。 |
| Gradle / AGP / Kotlin 版本 | 已锁定并构建验证：Gradle 9.8.2、AGP 9.4.1、Kotlin 2.4.21、KSP 2.3.12、Hilt 2.60.1；其余版本见 Version Catalog。 |
| 依赖下载是否正常 | Google Maven、Maven Central 与 Gradle 官方分发实际下载成功；M0、M1 完整依赖解析、编译、测试和 APK 构建成功。 |
| 可用设备（adb devices） | 已启动 Pixel_7 / emulator-5554，Android API 35，1080×2400；WHPX 可用；M0 启动导航走查与 M1 的 5 个仪器测试通过。没有验证其他 API 或实体手机。 |

### 环境检查依据与恢复步骤（2026-10-24）

- 已完整阅读 `README.md`，并严格按顺序完整阅读 01～11 号文档，再读取 `PROGRESS.md`、`DECISIONS.md`；读取完成前没有写代码。
- 官方 SDK 索引 `https://dl.google.com/android/repository/repository2-3.xml` 返回 `platforms;android-37.0`、`platforms;android-37.1`、`platforms;android-37.2`；其中 37.2 位于 stable channel（`channel-0`）、无 codename、无 preview revision。不要选择另列的 beta 包。本机仅有 36.1 平台；已安装 37.0.0 Build Tools 或 37.0 系统镜像不等于安装了 SDK Platform。
- 请在 Android Studio → Tools → SDK Manager，确认 SDK Location 为 `D:\Android\Sdk`，在 SDK Platforms 中安装当前稳定 Android API 37 平台（本次官方索引列出的最新稳定包为 Android SDK Platform 37.2）；在 SDK Tools 中安装 Android SDK Command-line Tools (latest)，便于后续工具链检查。
- 安装完成后重新检查 SDK，再继续 M0 的 Git 初始化、工程与隐私检查任务。设备不是构建的前置阻塞：后续可尝试启动已有 Pixel_7；无法运行时设备验收如实标记未验证。
- Git 可用；`git ls-remote https://github.com/xichugeek/Lunatide.git` 成功且没有返回任何 refs；本地尚不是 Git 仓库。没有提交、推送、创建标签或 Release。

## 里程碑记录

### M8 · 备份与数据管理 —— ✅ 实现完成（2026-10-24）
- 完成内容：规范 v1 加密备份格式、后台 KDF、SAF 导出/导入、密码与预览、覆盖二次确认/事务合并、结果计数；CSV 明文警告/BOM/转义/语言与敏感列开关；共享完整数据清除；数据库无法解锁恢复页及加密暂存恢复。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 18，累计 127 tests，0 failures/errors）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（新增 3，累计 14 tests，0 failures/errors/skipped，API 35）。首次设备测试因模拟器已关闭失败，重新启动后完整重跑成功；未将该次失败标通过。
- JVM：头部与密文每字节篡改、错误密码/魔数/版本、空与 20 年虚构记录、生产 600000 次迭代、恶意 KDF/文件边界、安全设置排除、未知字段/枚举、经期并集/较新记录、CSV BOM/多行引号/公式文本防护全部通过。
- 仪器：覆盖与合并计数、设置失败后健康记录事务回滚；模拟 Keystore 丢失、恢复准备失败时原数据库字节保持一致，随后确认恢复可重新读取且文件仍加密；清除数据库/WAL/SHM/包装密钥/设置及 PIN 哈希。
- 设备实际流程（英文、虚构数据）：记录开始 → 设置备份密码 → SAF Downloads 保存 .lunatide → 输入 DELETE 清除 → 回引导 → 导入刚保存文件 → 错误密码失败 → 正确密码预览 1 次经期 → 二次确认覆盖 → 首页语义 Period day 1、结束按钮恢复；勾选 CSV 明文警告后 SAF 导出成功。
- 验收：A1～A5、E8 原文件保护/恢复、F1～F3、F5 ✅；F4 BOM/转义计算与设备导出 ✅，Excel/WPS 实际打开 ⏭ 未验证；F6 禁用备份与仪器卸载后新安装引导已检查，真实云恢复场景未验证；G8 ✅。进程中断恢复标记已实现，系统断电全过程未做故障注入。
- 决策：D-014；无新增运行时依赖，源码豁免为零。
- 提交：本次 `feat(backup): 实现加密备份恢复与本机数据管理`。
- M7 提交与推送：`03cdfe9` 已成功推送 `origin/main`。
- 下一步：M9 完整设置、关于与本地政策/许可、国际化和 UI 走查。
### M7 · 应用锁与隐私设置 —— ✅ 实现完成（2026-10-24）
- 完成内容：4–6 位 PIN 自绘键盘、两次确认、旧 PIN 验证后修改/关闭、PBKDF2 哈希、生物识别快捷方式、自动锁定与外部跳转豁免、持久化输错限制、防截屏开关、忘记 PIN 的两次确认清除入口；锁屏不渲染底层页面；引导 PIN 接通。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 7，累计 109 tests，0 failures/errors）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（新增 2，累计 11 tests，0 failures/errors/skipped）。Lint 首轮发现缩进与两项 warning 已修复，最终全套成功。
- 设备走查：设置两次 PIN → 离开到桌面并返回要求解锁；强制停止后冷启动要求解锁，UI 树不含首页记录；实际输错五次出现 30 秒冷却，立即强停重开仍在倒计时；忘记 PIN → 说明 → 删除二次确认 → 取消，仍回锁屏，无绕过。窗口 dumpsys 包含 SECURE，实际 adb 截图为全黑（本机临时文件）。
- 验收：A1～A5 ✅；E3 默认立即/冷启动 ✅（其他超时设备组合未穷举）；E4 ✅；E5 无 PIN 禁用生物识别、无系统密码回退已实现与静态核对，真实指纹/面容认证 ⏭ 未验证；E6 截图阻止 ✅、最近任务系统缩略图 ⏭ 未验证；E7 无绕过/二次确认 ✅，实际删除后恢复在 M8 完整验证；G8～G9 ✅。
- 决策：D-013；依赖均为 AndroidX，最终 debug/release Manifest 无网络权限，源码豁免为零。
- 提交：本次 `feat(security): 实现应用锁与持久化输错保护`。
- M6 提交与推送：`6153f56` 已成功推送 `origin/main`。
- 下一步：M8 加密备份、CSV、完整数据清除与数据库恢复页。
### M6 · 提醒 —— ✅ 实现完成（2026-10-24）
- 完成内容：5 种本地通知、双语言隐私正文与中性 publicVersion、按类型唯一 WorkManager 调度与每日兜底重算、数据/设置/前台日期变化重算、Worker 发送前复核；设置页与引导接入、用户主动申请权限、拒绝后引导及 ROM 说明。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 14，累计 102 tests，0 failures/errors）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（新增 2，累计 9 tests，0 failures/errors/skipped）。
- 设备走查：首次打开未弹通知权限；点开启通知才出现系统权限框；实际拒绝后显示权限未开启及去系统设置。中英文、全部五类通知的隐私正文与 publicVersion 由仪器测试核对。D2 两分钟实际送达、D4 系统锁屏渲染、D6 设备重启后送达 ⏭ 未验证，不以构建或通知对象测试代替。
- 验收：A1～A5 ✅（人工审阅依赖新增仅 AndroidX Work 相关）；D1、D3 ✅；D5 纯函数测试 ✅、设备送达未验证；G8～G9 ✅。临时加入状态存档 API 的负向验证按预期失败，恢复后全检查成功。debug/release Manifest 无网络权限、无额外导出页面。
- 决策：D-012；引导日期草稿移至 ViewModel 内存并加强禁用状态存档 API 检查；系统 JobService 仅允许系统签名权限保护的导出例外。
- 提交：本次 `feat(reminders): 实现离线隐私提醒与权限引导`。
- M5 提交与推送：`121e921` 已成功推送 `origin/main`。
- 下一步：M7 应用锁与隐私设置；定时送达和重启恢复设备验收仍待补充。
### M5 · 统计页 —— ✅ 完成（2026-10-24）
- 完成内容：概览、周期/经期长度 Canvas 柱图、最近 12 个完整记录、平均虚线与 21–35 天背景带、症状 Top 5、痛经分布、全部历史及点击跳转月份、温和提示、少于两次经期空状态；独立 StatsViewModel 与每根柱子语义描述；补齐每日记录经期开关无障碍标签。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 3，累计 88 tests，0 failures/errors）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅；仪器测试本次未重复执行（统计/UI 变更，M4 的 7 个通过结果保留）。
- 设备走查（虚构数据）：1 条经期时显示空状态；添加 2026-09-01 单日历史与 2026-10-24 进行中记录，页面平均周期 53 天、平均经期 1 天、规律性数据不足，与手算一致；点击 9 月历史实际跳转至 9 月日历。
- 验收清单：A1～A5 ✅；C5～C6 ✅（固定样例 UI + 纯计算测试）；G8～G9 ✅；G3 ⏭（语义已实现与检查，尚未实际用 TalkBack 完整走查）；G2/G5/G7 的完整设备走查待 M9。
- CI：修复后 aef710a 的 GitHub Actions 已成功执行四项检查：https://github.com/xichugeek/Lunatide/actions/runs/40053518315 。M0～M3 的旧失败记录保留，不改写历史；本次提交的新 CI 结果待推送后运行。
- 决策：D-011；未新增运行时依赖。
- 提交：本次 `feat(stats): 实现离线统计图表与周期历史`。
- M4 提交与推送：`aef710a` 已成功推送 `origin/main`。
- 下一步：M6 通知与本地提醒，补齐引导中的提醒启用。

### M4 · 日历 + 每日记录 —— ✅ 完成（2026-10-24）
- 完成内容：六周月网格、切月按钮/滑动/回到今天、周起始日、形状与颜色标记、日期无障碍描述、可折叠图例/月摘要；每日记录全部字段、经期开关与确认、300 ms 防抖、关闭前写入、空记录清理、未来只读与预测说明；备注与体温采用禁止个性化学习的原生输入组件。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 12，累计 85 tests，0 failures/errors）/ `lintDebug` ✅（0 error、仅原有图标目录 warning）/ `assembleDebug` ✅；`connectedDebugAndroidTest` ✅（新增经期+经量复合事务回滚，累计 7 tests，0 failures/errors/skipped）。最后一次仅调整 Sheet 可用窗口高度与空状态文案后重跑四项检查通过。
- 设备走查（API 35，虚构数据）：经量中、疲倦、平静、备注自动保存；关闭重开后备注逐字一致、首页摘要更新；未来日期只显示预测与提示，输入框数量为 0；同房默认隐藏。输入法实际报告 inputType=0xa0001、imeOptions=0x41000006，包含禁止个性化学习与禁止建议标志，未启用自动纠错。
- 验收清单：A1～A5 ✅；B5 领域/事务/编辑器测试 ✅（所有手势与设备组合尚未穷举）；B6 自动保存/关闭重开 ✅（体温边界另有 JVM 测试，同房启用后的设备验证待设置页）；B7 ✅；B8 默认隐藏 ✅；C2 形状标记与健康声明已实现；G3 ⏭（日期语义已检查，尚未实际启用 TalkBack）；G8～G9 ✅。字体 200% 与横屏完整走查待 M9。
- CI：发现 M0～M3 的远端构建在旧 setup-android 默认安装已移除的 `tools` 包时失败，未执行 Gradle 检查；已修复为官方 CLI 显式安装，等待本次推送后验证。历史本地检查结果仍有效，不将历史 CI 标为通过。
- 决策：D-009、D-010。未新增运行时依赖，源码禁用 API 豁免仍为零。
- 提交：本次 `feat(calendar): 实现日历与隐私每日记录编辑`。
- M3 提交与推送：`7cbc3bb` 已成功推送 `origin/main`。
- 下一步：M5 统计页，同时跟进修复后的 CI 结果。

### M3 · 首次引导 + 今天页 —— ✅ 页面与核心流程完成（2026-10-24）
- 完成内容：5 页引导框架、全程可跳过、可选历史日期与周期先验、未知状态、数据迁移说明；今天页欢迎/进行中/等待/当天/推迟全部状态、周期圆环、主按钮与触感/按压反馈、撤销和事务化改日期、近 10 天确认与点滴出血、今日记录摘要、未来预测和健康声明；返回前台刷新日期；中英文数量复数资源。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 12，累计 73 tests，0 failures/errors）/ `lintDebug` ✅（0 error、仅原有图标目录 1 warning）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（新增撤销保留备注用例，累计 6 tests，0 failures/errors/skipped，API 35）。
- 设备走查（虚构数据）：中文引导 → 跳过 → 首页欢迎 → 例假来了显示第 1 天 → 撤销回欢迎 → 再开始 → 改为昨天显示第 2 天 → 例假走了显示距离下次 → 撤销恢复进行中。日期选择器已实际确认未来日期 enabled=false。截图仅保存在本机临时目录，没有真实数据、未充当正式发布素材。
- 验收清单：A1～A5 ✅；B1 核心流程 ✅（≤10 秒未严格计时）；B2～B3 ✅；B4 ViewModel/领域测试 ✅（设备确认弹窗走查仍待补充）；C3～C4 首页计算与显示 ✅；G4 当前页面的自有文案中英文齐全；G8～G9 ✅。TalkBack、200% 字号及其他设备未验证。
- 决策：D-007、D-008；中文语言回退问题在模拟器复现后修复。当前 optional 安全/提醒页只展示说明，M6/M7 必须接入实际启用与 PIN 设置；每日记录完整编辑在 M4 接入，设置页完整条目在 M9 接入。以上未实现功能不标完成。
- 提交：本次 `feat(today): 实现首次引导与首页记录流程`。
- M2 提交与推送：`7753cad` 已成功推送 `origin/main`。
- 遗留问题：引导可选提醒/PIN 集成待 M6/M7，圆环细节与全局无障碍继续在 M9 打磨；Compose UI 自动化在 M9 加入。
- 下一步：M4 日历与每日记录。

### M2 · 预测引擎与统计 —— ✅ 完成（2026-10-24）
- 完成内容：纯 Kotlin 预测/统计、最近 12 个周期硬过滤与中位数离群过滤、最近 6 个加权平均、先验/手动优先、置信度和范围、进行中上限、固定原始预测与推迟顺延、易孕期裁剪与隐藏原因、症状 Top 5、痛经分布、规律性和温和提示。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 31，累计 61 tests，0 failures/errors）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅；仪器测试本次未重复执行（纯 Kotlin 计算变更，M1 结果保留）。
- 验收清单：A1～A5 ✅；C1 ✅（06 §8 全部 25 个场景按 t01～t25 对应）；C3～C4 计算逻辑 ✅（UI 待 M3/M4）；C5 固定样例计算 ✅（统计页待 M5）；G8 ✅。20 年虚构数据测试为 260 次经期、7300 条日志，计算可完成；滚动流畅度尚未验证。
- 决策：D-006（标准差、日期倒退、数据范围与排序复杂度说明）。
- 提交：本次 `feat(prediction): 实现周期预测与统计并覆盖规范场景`。
- M1 提交与推送：`da79d2c` 已成功推送 `origin/main`。
- 遗留问题：上述预测还未接入 UI；不能以 JVM 性能结论替代中端真机体验验证。
- 下一步：M3 首次引导、今天状态卡、操作撤销/改日期与 ViewModel 测试。

### M1 · 数据层 —— ✅ 完成（2026-10-24）
- 完成内容：三张 Room 表与 DAO、显式领域映射、稳定枚举、百分度体温、schema v1 导出；SQLCipher + Keystore AES-GCM 包裹随机口令；可注入工厂、非破坏性错误；DataStore 设置与可注入 TodayProvider；周期/日志 Repository 与事务化经期编辑。
- 测试：新增 30 个 JVM 测试覆盖 05 §3 的新建、进行中限制、相邻扩展、桥接合并、历史补录、近 10 天确认、首尾/中间/单日删除、未来日期、体温备注边界、跨年闰年等；5 个真实仪器测试覆盖 SQLCipher 读写重开、错误口令、非明文文件头、密钥丢失/包裹文件缺失保留数据库、经量清理保留点滴、内存 Room 与事务回滚。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（30 tests, 0 failures）/ `lintDebug` ✅（0 error、原有 1 warning）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（5 tests, 0 failures/0 errors/0 skipped，Pixel_7 API 35）。
- 验收清单：A1～A5 ✅；B5 领域规则 ✅（日历 UI 尚未实现，完整条目待 M4）；E1～E2 ✅；E8 数据保留逻辑 ✅（恢复 UI 待 M8）；G8 ✅；其他 UI/提醒/锁/备份/发布项待对应里程碑。
- 决策：D-004、D-005。DB 口令调用方缓冲区及时清零，连接池工作副本在关闭时清零；不以清零破坏连接池后续连接。
- 提交：本次 `feat(data): 实现加密数据层与经期编辑规则`；待推送结果补记。
- M0 提交与推送：`3b52626` 已成功推送 `origin/main`。
- 遗留问题：仪器测试仅覆盖 API 35 模拟器；其他设备与完整功能验收后续完成。
- 下一步：M2 预测引擎与统计，覆盖 06 §8 全部 25 个场景。

### M0 · 工程初始化与隐私防线 —— ✅ 完成（2026-10-24）
- 完成内容：Git/main/远端；Gradle Wrapper（分发 SHA-256）、Version Catalog、单 app、Compose/Material 3/Hilt、类型安全三个导航占位；Manifest 与备份排除加固；三个隐私任务并接入 check；push/PR CI；AGENTS、README、PRIVACY、MIT LICENSE、SECURITY、CHANGELOG、两个 Issue 模板；版本号自动计算；初版矢量自适应图标。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（任务 NO-SOURCE，0 个测试，业务测试从 M1 开始）/ `lintDebug` ✅（0 error、1 warning，D-003）/ `assembleDebug` ✅。
- 故意失败验证：临时将 INTERNET 从移除标记改为实际权限；`verifyPrivacy` 以“debug 出现未授权权限 android.permission.INTERNET”失败，退出码非零。finally 恢复 Manifest，再运行四项检查全部成功。未安装或提交含网络权限的 APK。
- 设备：Pixel_7 / emulator-5554 / API 35 安装 debug APK 成功，冷启动 Status ok；UI 层级确认日历与统计导航页。尚未运行仪器测试套件（M0 未编写）；首次 debug 启动命令 TotalTime=2247 ms，不据此宣称满足正式版冷启动目标。
- 验收清单：A1～A5 ✅（两个最终 Manifest、全部备份排除域、依赖清单人工检查、源码无用户日志）；A6 ⏭（M10）；A7～A8 ⏭（完整权限设置与飞行模式功能走查留待功能就绪）；G8 ✅（warning 说明见 D-003）；G9 ✅（当前导航界面提供浅/深色 Preview）；H3、H6 ✅；其余业务与发布项未实现，不标记通过。
- 决策：D-001～D-003。依赖报告位于 `app/build/reports/privacy/dependencies.txt`（构建产物不提交）。
- 截图或说明：模拟器实机界面已检查，仅为空导航占位；截图未作为正式宣传素材。debug APK 约 11.39 MiB，不作为 release 体积结论。
- 遗留问题：完整业务、设备验收、release 构建与签名按后续里程碑实施。
- 提交：本次 `chore(build): 初始化安卓工程与隐私防线`；推送结果在后续日志补记。
- 下一步：M1 数据层。

### 环境修复与 M0 恢复 —— 🚧 进行中（2026-10-24）
- 用户明确授权自行修复环境；已安装官方 SDK Command-line Tools 23.0、稳定 SDK Platform 37.2、Temurin JDK 17.0.20.1；JBR 21 继续作为 Gradle 运行环境。工具均在仓库之外。
- Gradle 9.8.2 下载并按官方 SHA-256 校验，生成包含分发摘要的 Wrapper。命令行工具按 SDK 官方索引 SHA-1 校验。Android CLI 使用 `--no-metrics`，SDK 许可证已存在。
- SDK 新版 `sdkmanager` 会转交 Android CLI，旧参数未安装平台；随后依据 `android sdk install --help` 使用新版命令完成安装。
- 启动已有 Pixel_7，`adb devices -l` 返回 `emulator-5554 device`，`sys.boot_completed=1`；Android API 35，1080×2400。仅验证设备启动，App 功能尚未验证。
- Git 已初始化 main，配置指定远端并 fetch（远端无 refs）；提交身份使用项目公开别名与 GitHub noreply 地址。
- 已创建 M0 工程、Manifest 加固、三个隐私检查、CI 与开源基础文档，开始运行四项检查。尚未提交或推送。
- 决策：D-001、D-002。

### M0 · 工程初始化与隐私防线 —— ❌ 阻塞（2026-10-24）
- 完成内容：规范完整阅读；JDK、SDK、仓库端点、设备及 Git 远端只读检查；将事实和恢复步骤记录在本文件。
- 尚未实现：工程、隐私任务、CI、页面及其他 M0 交付物。没有开始 M1～M10。
- 验证：`verifyPrivacy` ⏭ / `testDebugUnitTest` ⏭ / `lintDebug` ⏭ / `assembleDebug` ⏭（均未运行：环境检查发现所需 SDK Platform 缺失，尚无 Gradle 工程）；仪器测试 ⏭（无已连接设备）。
- 验收清单：A1～A4 ⏭（尚无工程与合并 Manifest / 依赖报告）；A5 ⏭（尚无 App 源码）；A6 ⏭（M10）；A7～A8 ⏭（设备未验证）；G8～G9、H6 ⏭（M0 尚未实现）。其余功能项未实现，不标记通过。
- 决策：无新增实现决策；依照规范停止，不自行降级 SDK。
- 遗留问题：需人类补齐 SDK Platform 后继续。
- 提交：无；仅更新本进度文件，M0 未达到提交并推送的完成条件。

<!-- 模板：
### M0 · 工程初始化与隐私防线 —— ✅ 完成 / 🚧 进行中 / ❌ 阻塞（YYYY-MM-DD）
- 完成内容：
- 验证：verifyPrivacy ✅ / testDebugUnitTest ✅ (N tests) / lintDebug ✅ / assembleDebug ✅ / connectedAndroidTest ⏭（原因）
- 验收清单：A1 ✅ A2 ✅ …
- 遗留问题：
- 提交：<commit hash 简写>
-->

## 待人类确认

- [x] 开源许可证：MIT（已确定）
- [x] 发布渠道：GitHub Releases + 官网项目页（已确定）
- [ ] release 签名密钥（M10 时由人类生成）
- [ ] 官网项目页最终 slug（计划 `/projects/lunatide/`）
