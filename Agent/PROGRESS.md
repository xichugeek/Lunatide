# 开发进度（由 Codex 维护）

> 每个里程碑结束时更新。最新的放在最上面。不要删除历史记录。

## 当前状态

- 当前里程碑：M0～M9 已实现并推送；M10 本地构建、中文素材与发布准备已就绪，等待人类生成正式签名，不标 M10 完成
- 最近一次构建：`verifyPrivacy testDebugUnitTest lintDebug lintRelease assembleDebug assembleRelease` 成功（2026-10-24）；129 个 JVM 测试、15 个仪器测试通过；两个变体 Lint 均为 0 error、2 warnings（D-003 / D-015）
- 签名设置：用户已授权代执行命令，已启动“月汐 · 正式签名”本机交互窗口，由用户直接输入密码后生成密钥及被忽略的 keystore.properties；不需要向聊天发送密码。尚未推送 tag、创建 Release 或部署官网。

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

### M10 继续 · 本机交互签名设置 —— 🚧 等待窗口内输入（2026-10-24）
- 用户明确授权代执行上一轮签名命令。新增 setup-signing.ps1 并已在独立可见 PowerShell 窗口启动，固定项目证书主体，不要求填写个人姓名/住址；用户在窗口输入密码，代理不读取密码或配置内容。
- 本机配置由用户再次输入密码并校验成功后创建；密钥在仓库外、配置被 Git 忽略，均不覆盖已有文件。输入为空或校验失败不会写入配置；脚本不包含密码、不创建 tag/Release。
- 验证：PowerShell 语法解析 ✅；CheckOnly 实际工具/状态检查 ✅（不生成密钥）；虚构的空格、反斜杠、中文、换行、制表符经实际 JDK Properties.load 往返 ✅。没有修改 Android App 源码，本轮不重复宣称执行 Android 测试；上轮 129 JVM/15 仪器及构建结果保留。
- CI 补记：发布准备 f471888 和进度 ff07a05 的 Actions 均已 success，后者 https://github.com/xichugeek/Lunatide/actions/runs/40066778291 。历史“正在运行”记录保留。
- 目前仅检查签名文件/配置是否存在，不读取内容；窗口正等待用户输入。尚未生成正式 APK 校验元数据，M10 仍未完成。
- 决策：D-017。配置完成后继续 release.ps1 → 校验最终包 → 更新官网素材；公开 tag/Release 仍待单独明确同意。

### M10 · 发布准备 —— 🚧 已完成可独立执行的准备，待正式签名（2026-10-24）
- 完成内容：1.0.0 / 10000；R8/资源压缩/SQLCipher JNI keep；正式签名发布脚本和官网元数据更新脚本；新手构建与发布指南；README、SECURITY、CHANGELOG 定稿；debug 专用 8 周期/20 年虚构数据及正式源集隔离；5 张中文真实运行截图、512px 图标、12 节官网文案草稿与模板。
- 修复：实际安装 R8 包时发现运行时类名比较导致底部导航隐藏，已改用类型安全 hasRoute 并实际检查三个 Tab；日历缓存日期索引，避免每格重复扫描 7300 条日志；设置/提醒/安全/每日记录开关整行可点击，合并无障碍焦点。点击文字开启同房记录后编辑页实际出现选项。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（本次新增 0，累计 129 tests，0 failures/errors/skipped）/ `lintDebug` ✅ / `assembleDebug` ✅；另外 `lintRelease` ✅ / `assembleRelease` ✅ / `connectedDebugAndroidTest` ✅（15 tests，0 failures/errors/skipped，API 35）。最后开关调整后已重新构建并完整重跑设备套件。两变体 Lint 各 0 error、2 warnings，保留原因见 D-003/D-015。
- 最终测试 APK：10,118,356 字节（约 9.65 MiB），仅本机 debug 证书签名，不是正式发布包；最终 APK 权限不含网络权限。反汇编实际 DEX，Log.v/d/i/w/e 调用为 0；DEX/resources 不含示例入口；最终包的政策和许可 assets 与根目录原文件字节一致。
- 升级：在忽略的 build/upgrade-test 中由 M9 源码构建旧 0.1.0 / 100，安装并记录经期/中等经量，再以同一测试证书覆盖 1.0.0 / 10000，记录与经量保留；正式证书尚不存在，未宣称正式签名升级已验证。
- 性能：API 35 模拟器、本地 R8 包，8 周期冷启动首帧 366/363/315ms，记录就绪并两帧后 FullyDrawn 为 1664/917/1221ms；260 次经期/7300 条日志的 FullyDrawn 为 2201/1259/2120ms。未把首帧当可交互时间，未满足“中端真机 ≤1.5s”的验证条件。日历索引优化后 8 次切月 374 帧、jank 2.14%、P90/P95 18ms；统计 5 次滚动 159 帧、jank 3.14%、P90/P95 18ms；仅为模拟器有限样本。
- 补充设备验收（均为虚构数据）：飞行模式下中文日历/统计/每日记录可用、近期经期确认后首页显示点滴；通知权限页实际仅列通知。临近提醒设为随后第 2 个分钟，12:05:00 实际收到中性正文；每日提醒设为 13:02，重启后未打开 App，13:02:12 收到 id=103。临时时区/日期仅用于测试；完成后恢复真实时间、上海时区、100% 字号、竖屏、关闭 TalkBack 与演示模式，App 保持简体中文。
- 截图：docs/site/screenshots/01～05，1080×2400、简体中文、浅色、虚构示例；图标 512×512。所有使用处注明虚构数据。正式 release-info.json 未生成，官网第 4 节明确待正式签名，不填假大小/哈希/指纹。
- 脚本：3 个 PowerShell 脚本语法检查通过；update-site 在没有真实 release-info.json 时已实际按预期停止。提交后在干净 main 工作区实际执行 release.ps1，退出码 1、明确提示“缺少 keystore.properties，正式签名密钥必须由人类生成和保管”，没有创建正式元数据。此为拒绝缺少签名的门槛验证通过，不能写成发布全流程通过；正式 APK/sha256/Release Notes 的生成仍待人类签名。
- 提交与推送：发布准备 `f471888` 已成功推送 origin/main；本条补记签名门槛和最终验收状态。
- CI：M9 `3aec2b6` 已 success：https://github.com/xichugeek/Lunatide/actions/runs/40062444267 。发布准备 `f471888` 的 https://github.com/xichugeek/Lunatide/actions/runs/40066731154 正在运行，尚不标通过。
- 仓库自查：无新增运行时依赖；权限白名单与依赖报告已人工检查；无源码隐私豁免；规范仅更新 PROGRESS/DECISIONS；未提交个人路径、密钥、APK、备份或真实记录；无 tag/Release 操作。
- 决策：D-016。下一步仅在人工配置正式签名后运行 release.ps1、核对真实校验信息并更新官网素材，再单独请求 tag/Release 的明确同意。

#### M10 全量验收复核（✅ 已有对应证据；⏭ 未验证或只完成部分，不代表全部通过）

| 条目 | 结果与依据 |
|---|---|
| A1 | ✅ 最后一次 verifyPrivacy 全部子任务通过，包含示例隔离。 |
| A2 | ✅ debug/release 合并 Manifest 和最终 R8 APK 均无 INTERNET/ACCESS_NETWORK_STATE。 |
| A3 | ✅ allowBackup=false；云备份/设备迁移的全部 5 个数据域排除。 |
| A4 | ✅ 人工检查实际 dependencies.txt；仅白名单和 D-002/D-004 已审计的基础传递依赖。 |
| A5 | ✅ 主源码无健康记录日志，禁用 API 检查通过。 |
| A6 | ✅ 最终 DEX 实际反汇编，Log.v/d/i/w/e 调用为 0。 |
| A7 | ✅ API 35 系统应用权限页实际只列 Notifications。 |
| A8 | ⏭ 飞行模式下核心浏览、记录与提醒已实测；未在飞行模式逐项重跑所有 SAF/安全流程。 |
| B1 | ⏭ 全新引导跳过→开始→第 1 天已通过 Compose 仪器测试；≤10 秒未严格单独计时。 |
| B2 | ✅ M3 实际开始/结束后显示距离下次；纯状态测试通过。 |
| B3 | ✅ M3 撤销/改日期设备走查，未来日期禁用。 |
| B4 | ✅ M10 实际显示距离上次结束 4 天确认，点“只是少量出血”后首页显示点滴。 |
| B5 | ✅ 全部领域规则单测、复合事务回滚仪器测试及日历编辑实测；未穷举所有设备手势。 |
| B6 | ⏭ 经量/症状/心情/备注保存及重开实测；体温边界有单测，体温/同房全部实际编辑组合未穷举。 |
| B7 | ✅ 未来只读实测：输入框 0 个；编辑器与日期规则单测通过。 |
| B8 | ✅ 默认隐藏，设置开启后编辑页实际出现同房选项；完成后示例设置恢复默认。 |
| C1 | ✅ 06 §8 的 t01～t25 全部实现并通过。 |
| C2 | ✅ 实际截图同月显示记录/虚线预测/易孕期/排卵日与声明，纯计算测试通过。 |
| C3 | ✅ M10 临时向后调 40 天，首页显示“已推迟 20 天”，日历预测从当天开始；原始预测日期未改写。 |
| C4 | ✅ 纯函数覆盖范围限制；M10 手动设 40 天后实际显示“周期不在 21–35 天范围内，暂不显示易孕期预测”，随后恢复默认。 |
| C5 | ✅ M5 固定样例与手算核对，统计单测通过。 |
| C6 | ✅ M5 单条记录空状态实际走查。 |
| D1 | ✅ M6 首开不弹、主动开启才请求、拒绝后显示系统设置引导，实际走查。 |
| D2 | ✅ 本地 R8 包配置临近提醒，目标分钟 12:05 实际送达。 |
| D3 | ✅ 五种中英文隐私正文仪器测试；实际送达正文为中性文案。 |
| D4 | ⏭ publicVersion 中性内容已在实际通知对象核对；带安全锁的系统锁屏渲染未验证。 |
| D5 | ⏭ 已记录后的抑制条件与调度纯测试通过；该条件的设备实际等待送达未验证。 |
| D6 | ✅ API 35 飞行模式下重启后未开 App，13:02:12 收到每日提醒；未验证实体机厂商省电限制。 |
| E1 | ✅ SQLCipher 实际仪器读写/错误口令/非 SQLite 明文头检查。 |
| E2 | ✅ no_backup 包装密钥与缺失时原 DB 保留的仪器测试。 |
| E3 | ✅ M7 立即锁/冷启动实际走查；其他超时与设备组合未穷举。 |
| E4 | ✅ 连续 5 次错误后 30 秒，强停重开仍冷却，实际走查。 |
| E5 | ⏭ 无 PIN 禁用及拒绝系统密码回退已核对；真实指纹/面容认证未验证。 |
| E6 | ⏭ FLAG_SECURE 与全黑截图已实测；最近任务系统缩略图未单独验证。 |
| E7 | ✅ 忘记 PIN 无绕过、删除两次确认实测；完整清除另见 F5。 |
| E8 | ✅ 仪器模拟 Keystore 丢失，恢复准备失败时原文件保持，确认后恢复成功。 |
| F1 | ✅ BackupCodec JVM 测试：往返/错误密码/逐字节篡改/头部/上界/20 年数据。 |
| F2 | ✅ M8 实际 SAF 导出→清除→错误密码→正确预览→覆盖恢复，记录与状态一致。 |
| F3 | ✅ 合并并集/较新记录规则、计数与事务回滚的 JVM/仪器测试。 |
| F4 | ⏭ BOM/CRLF/转义/公式文本单测与实际 CSV 导出完成；Excel/WPS 实际打开未验证。 |
| F5 | ✅ 完整清除后的引导实测；数据库/WAL/SHM/包装密钥/设置/安全状态清理与任务取消检查。 |
| F6 | ⏭ 仪器卸载后的新安装无记录/回引导已观察，禁止备份已检查；真实云恢复链路未验证。 |
| G1 | ⏭ 核心页面浅/深色实测，所有页面与主题的组合未穷举。 |
| G2 | ⏭ 200% 字号首页/日历核心页已实际检查；全部对话框/输入法组合未穷举。 |
| G3 | ⏭ TalkBack 实际启用和语义检查完成；真人朗读与完整手势流程未验证。 |
| G4 | ⏭ 所有资源键英文配对单测、M8 英文备份/恢复流程通过；全页人工英文走查未穷举，交付固定中文。 |
| G5 | ⏭ 200% 横屏核心页实测、内容最大 600dp；独立平板实体机未验证。 |
| G6 | ⏭ 中端真机未验证；模拟器 FullyDrawn 含超过 1.5s 样本，不能宣称达标。 |
| G7 | ✅ 20 年数据实际 R8 日历/统计操作与有限帧样本见上；其他硬件表现未验证。 |
| G8 | ✅ debug/release Lint 均 0 error，2 warnings 有明确保留原因。 |
| G9 | ✅ 页面含浅/深色 Preview，使用虚构状态，设备截图不是 Preview 伪造。 |
| H1 | ✅ 本地测试证书 assembleRelease 成功，10,118,356 字节；正式签名构建待人类。 |
| H2 | ✅ 最终 APK 通过 aapt2 权限检查。 |
| H3 | ✅ keystore.properties 不存在且被忽略；无密钥或签名密码入库。 |
| H4 | ✅ README 隐私承诺与根目录政策一致；App assets 字节同源。 |
| H5 | ✅ 健康声明本地完整、许可包含 SQLCipher 全文；运行时坐标检查及许可资产同源通过。 |
| H6 | ✅ MIT/SECURITY/CHANGELOG/两类 Issue 模板齐全，发布文档明确待签名与未发布状态。 |
| H7 | ⏭ 待人类正式签名；未生成或分发冒充正式包的 dist 产物。 |
| H8 | ⏭ 正式 APK/sha256 文件尚未生成，未伪造校验结果。 |
| H9 | ✅ src/debug 隔离任务、release 空插槽及最终 DEX/resources 扫描通过。 |
| H10 | ⏭ 12 节文案、5 张虚构截图与双语标题、512px 图标齐全；真实 release-info.json 待正式签名。 |
| H11 | ✅ 关于页实际版本与固定地址纯文本/复制，不打开浏览器。 |
| H12 | ✅ 同一测试签名从 0.1.0 / 100 覆盖至 1.0.0 / 10000 后记录保留；正式证书首次签发待人类。 |

### M9 · 设置、打磨与国际化 —— ✅ 实现完成（2026-10-24）
- 完成内容：周期自动/手动、记录项、一周起始日、提醒/安全/数据入口、主题、关于版本/纯文本复制地址、本地隐私政策/健康声明/开源许可；根目录政策与许可由构建原样打包，许可逐项覆盖解析运行时版本并纳入检查；英文资源完整配对。
- 按用户反馈重点重做中文视觉：首页月夜靛卡片、清晰大数字与月相、柔和玫瑰/潮汐青、同卡主操作、暖白卡片、品牌日期头；日历箭头与中文年月/窄星期；统一二级页面标题、设置列表卡片、浅/深色容器色。补齐启动图标与 core-splashscreen；不持久化健康草稿。
- 验证：`verifyPrivacy` ✅ / `testDebugUnitTest` ✅（新增 2，累计 129 tests，0 failures/errors）/ `lintDebug` ✅（0 error、2 warnings：图标目录和 Android 13+ localeConfig，分别见 D-003/D-015）/ `assembleDebug` ✅ / `connectedDebugAndroidTest` ✅（新增 1 个真实 Compose 核心流程，累计 15 tests，0 failures/errors/skipped）。前两轮 UI 测试分别发现 JUnit 返回类型与子串断言写法问题，均修复并完整重跑，未忽略失败。
- Compose UI 实测：全新引导跳过 → 记录开始 → 中文日历经期格 → 修改经量中 → 关闭自动保存 → 日期语义包含已记录经量；资源单测检查所有中文键有英文同类型资源及 plurals other 项。
- 设备走查：中文首页与设置、浅色/深色、本地政策、200% 系统字体的首页与日历、200% 横屏居中与滚动，未见核心文字重叠或崩溃。走查后已恢复字号 100%、竖屏和中文。示例截图仅临时保存，正式素材待 M10。
- TalkBack：实际开启服务并确认 touch exploration，检查到核心语义与操作入口；ADB 注入点击与真人触摸探索行为不同，未完成完整人工朗读/手势验证，因此 G3 ⏭ 未验证（不将语义测试充当人工 TalkBack 全流程通过）。走查后已关闭测试服务并还原该设置。
- 验收：A1～A5、H4 中文本地政策与根目录同源、H5 许可同源、H11 地址仅复制、G8～G9 ✅；G1/G2/G5 核心页走查 ✅，所有页面/设备组合未穷举；G4 英文资源齐全且 M8 英文核心导出恢复走通，M9 全页英文人工走查未穷举；G6 release 冷启动和 G7 20 年真实界面滑动仍待验证。
- CI：M5/M6/M7/M8 均已成功：M8 https://github.com/xichugeek/Lunatide/actions/runs/40060664084 （其余对应提交的 Actions 也核对 success）。本次推送后继续检查 CI。
- 决策：D-015；仅新增白名单 AndroidX SplashScreen 运行时依赖；Compose UI 测试库只用于 androidTest。
- 提交：本次 `feat(ui): 打磨中文月汐界面与完整设置`。
- M8 提交与推送：`9ce11a4` 已成功推送 `origin/main`。
- 下一步：M10 R8 release 验证、发布脚本/指南、虚构示例数据与官网素材；正式签名必须由人类生成与保管，尚未配置。
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
