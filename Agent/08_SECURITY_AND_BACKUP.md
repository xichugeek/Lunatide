# 08 · 安全：数据库加密、应用锁、加密备份

## 1. 数据库加密（SQLCipher + Android Keystore）

### 1.1 方案
1. 首次启动时生成 32 字节随机数据库口令：`SecureRandom().nextBytes(ByteArray(32))`。
2. 在 Android Keystore 中生成（或读取）AES-256-GCM 密钥，别名 `lunatide_db_kek`：
   - `KeyGenParameterSpec`：`PURPOSE_ENCRYPT | PURPOSE_DECRYPT`，`BLOCK_MODE_GCM`，`ENCRYPTION_PADDING_NONE`，`setKeySize(256)`。
   - **不**设置 `setUserAuthenticationRequired(true)`——后台提醒 Worker 需要在用户未解锁 App 时读取数据。
   - 设备支持时可尝试 `setIsStrongBoxBacked(true)`，失败则回退普通 Keystore（捕获 `StrongBoxUnavailableException`）。
3. 用该 Keystore 密钥加密数据库口令，把 `IV(12B) + 密文` 写入 `context.noBackupFilesDir/db_key.bin`。
4. 每次打开数据库：读文件 → Keystore 解密 → 得到口令 → 传给 SQLCipher 的 `SupportOpenHelperFactory`（`net.zetetic.database.sqlcipher`）→ 用后尽快将口令 `ByteArray` 填 0。
5. 需要先 `System.loadLibrary("sqlcipher")`（在 Application 或 DB 模块初始化时）。

### 1.2 失败处理
- `db_key.bin` 存在但 Keystore 解密失败（`KeyPermanentlyInvalidatedException`、`UnrecoverableKeyException`、`AEADBadTagException` 等）→ 进入「数据无法解锁」恢复页（见 `04_FEATURE_SPEC.md` §9），**禁止**自动删除数据库。
- 数据库文件存在但 `db_key.bin` 不存在 → 同上。
- 两者都不存在 → 视为全新安装，正常初始化。

### 1.3 测试
- JVM/Robolectric 测试：使用不加密的 Room 内存数据库（通过 DI 替换 `openHelperFactory`）。
- 仪器测试（`androidTest`）：真实 SQLCipher 打开、写入、关闭、重新打开、读取一致；用错误口令打开必须失败。
- 若当前环境没有模拟器/真机，仪器测试写好但标注「未在本机运行」，并记录在 `PROGRESS.md`。

## 2. 应用锁

### 2.1 PIN
- 4–6 位数字，设置时输入两次确认。
- 存储：`PBKDF2WithHmacSHA256`，16 字节随机盐，迭代次数 ≥ 120,000（在中端机上耗时约 100–300ms，在 `Dispatchers.Default` 执行并显示加载态），输出 32 字节。存 `pin_hash`、`pin_salt`、`pin_iterations`。
- 比对用常量时间比较（`MessageDigest.isEqual`）。
- **说明**：PIN 是界面层的访问控制，不参与数据库加密（否则后台提醒无法工作）。这一点在隐私政策中如实说明。

### 2.2 生物识别
- `androidx.biometric.BiometricPrompt`，`BIOMETRIC_STRONG`（不可用时允许 `BIOMETRIC_WEAK`，记录决策）。
- **不允许**用系统锁屏密码（`DEVICE_CREDENTIAL`）代替 App PIN——场景上，身边的人可能知道手机锁屏密码。
- 生物识别只是 PIN 的快捷方式：开启生物识别前必须已设置 PIN；生物识别连续失败或被取消时回到 PIN 键盘。

### 2.3 锁定时机
- 冷启动：锁定。
- App 进入后台（`ProcessLifecycleOwner` 的 `ON_STOP`）记录 `SystemClock.elapsedRealtime()`；回到前台时若超过 `lock_timeout_seconds` 则锁定（0 = 立即）。
- 打开系统文件选择器（导入/导出备份）、系统通知权限弹窗等**由 App 自身发起**的跳转返回时，不重新锁定（用一个“豁免一次”标记实现）。
- 锁屏期间，App 内容不得在锁屏页下方被渲染出来（避免切换动画中闪现数据）。

### 2.4 输错限制
- 连续错 5 次 → 锁定 30 秒；之后每再错 5 次，锁定时长翻倍（上限 30 分钟）。使用 `elapsedRealtime` 计时，持久化到 `security.preferences_pb`，防止重启绕过（重启后 elapsedRealtime 归零的情况：若检测到归零，按“剩余时长”重新计时）。
- **永不因输错而自动删除数据。**

### 2.5 忘记 PIN
- 锁屏页提供「忘记密码？」→ 说明：「为了保护你的数据，月汐无法找回密码。你可以清除全部数据并重新开始；如果之前导出过备份，清除后可以从备份恢复。」→ 二次确认（输入「删除」）后执行「删除全部数据」流程。

### 2.6 防截屏
- `secure_screen = true` 时对 `MainActivity` 的 window 设置 `WindowManager.LayoutParams.FLAG_SECURE`（同时让最近任务缩略图变为空白）。设置变更立即生效。

## 3. 加密备份文件（`.lunatide`）

### 3.1 用户流程
**导出**：设置 → 数据 → 导出加密备份 → 设置备份密码（≥ 6 位，任意字符，输入两次；提示「这个密码无法找回，请务必记住」）→ 系统文件保存器（`ActivityResultContracts.CreateDocument("application/octet-stream")`），默认文件名 `lunatide-backup-20261024-2130.lunatide` → 完成后提示「备份已保存。你可以通过微信文件传输、数据线等方式把它复制到别的地方保存」。

**导入**：设置 → 数据 → 从备份恢复 → 系统文件选择器（`OpenDocument`，MIME 用 `*/*`，因为很多文件管理器识别不了自定义扩展名）→ 输入备份密码 → 预览（「这个备份包含 24 次经期、312 天记录，导出于 2026-10-24」）→ 选择方式：
- **覆盖**：清空现有数据后导入（默认，二次确认）。
- **合并**：保留现有数据，把备份中的数据合并进来（规则见 §3.4）。

### 3.2 明文内容（加密前）
UTF-8 JSON，使用 kotlinx.serialization：

```json
{
  "format": "lunatide-backup",
  "formatVersion": 1,
  "exportedAt": "2026-10-24T21:30:00+08:00",
  "appVersionName": "1.0.0",
  "appVersionCode": 1,
  "periods": [
    { "start": "2026-09-05", "end": "2026-09-10", "updatedAt": 1790000000000 }
  ],
  "dayLogs": [
    {
      "day": "2026-09-05", "flow": "MEDIUM", "pain": 2, "mood": "LOW",
      "symptoms": ["CRAMPS", "FATIGUE"], "bbtCentiCelsius": 3652,
      "intercourse": null, "note": "……", "updatedAt": 1790000000000
    }
  ],
  "settings": { "manualCycleLength": null, "showFertility": true, "...": "..." }
}
```

- 日期用 ISO `yyyy-MM-dd` 字符串（与数据库 epochDay 解耦，人类可读）。
- `settings` 只包含周期/记录/外观/提醒相关设置；**不包含** PIN 哈希、应用锁设置、数据库密钥。
- 导入时忽略未知字段（`ignoreUnknownKeys = true`），未知枚举值丢弃该字段而不是整条失败。

### 3.3 文件二进制格式

```
offset  size  内容
0       8     魔数 ASCII "LUNATIDE"
8       1     文件格式版本 = 0x01
9       1     KDF 算法 id：0x01 = PBKDF2-HMAC-SHA256
10      4     KDF 迭代次数（uint32，大端），v1 取 600000
14      16    salt（随机）
30      12    AES-GCM IV/nonce（随机）
42      ...   密文 + 16 字节 GCM tag
```

- 明文 = `GZIP(JSON)`。
- 密钥 = `PBKDF2-HMAC-SHA256(password, salt, iterations, 256 bit)`。
- 加密 = `AES-256-GCM`，**AAD = 文件前 42 字节（头部）**，防止头部被篡改。
- 导出/导入的 KDF 运算在后台线程执行，显示进度（低端机可能需要数秒）。
- 解密失败（`AEADBadTagException`）→ 提示「密码不正确，或者文件已损坏」，不区分两者。
- 魔数不对 → 「这不是月汐的备份文件」。版本号大于当前支持 → 「这个备份来自更新版本的月汐，请先升级 App」。
- 实现放在 `data/backup/BackupCodec.kt`，纯 Kotlin + `javax.crypto`，**可以在 JVM 单元测试中完整测试**：
  - 加密→解密往返一致
  - 错误密码失败
  - 篡改头部任意一字节失败、篡改密文任意一字节失败
  - 魔数/版本错误给出对应错误类型
  - 空数据备份可导出导入
  - 大数据（20 年数据）往返一致
  - 测试中可把迭代次数参数化调低以加快速度（生产代码固定 600000）

### 3.4 合并规则
- **经期**：把备份经期与现有经期放在一起，按 `05_DATA_MODEL.md` 的不变量重新整理：有重叠或相邻的合并为一段（取并集）；若同一段在两边都存在，以 `updatedAt` 较新者的结束日为准。
- **DayLog**：按日期；同一天两边都有时，取 `updatedAt` 较新的整条记录。
- **设置**：合并模式下**不覆盖**现有设置；覆盖模式下覆盖。
- 合并完成后显示结果：「新增 3 次经期、45 天记录；更新 2 天记录」。
- 整个导入在**一个数据库事务**中完成，失败则完全回滚。

## 4. CSV 导出（明文）

- 入口带醒目警告：「CSV 文件没有加密，任何拿到文件的人都能看到内容。适合给医生查看或用表格软件分析。」需勾选「我知道了」才能继续。
- 两个文件不方便用 SAF 一次保存，因此导出**一个** CSV：每行一天（包含所有有记录的日期以及所有经期日），列：
  `日期, 是否经期, 经期第几天, 经量, 痛经, 症状, 心情, 基础体温, 同房, 备注`
  （「同房」列只在设置中开启了同房记录时导出。）
- 编码 UTF-8 **带 BOM**（Excel/WPS 打开中文不乱码）；字段按 RFC 4180 转义（逗号、引号、换行）。
- 列头和枚举值使用当前界面语言。

## 5. 安全相关的其他要求

- `MainActivity`：`android:exported="true"`（Launcher 需要）；其他组件一律 `exported="false"`。
- 不注册任何 `intent-filter` 接收外部数据（例如不响应“分享到月汐”），减少攻击面。v1.1 若要支持“用月汐打开 .lunatide 文件”，届时再评估。
- 不使用 `ContentProvider` 对外暴露数据（WorkManager/Startup 自带的 provider 设为不导出，默认即如此）。
- 截图/录屏保护见 §2.6；剪贴板只在用户点「复制」时写入非健康数据（例如开源地址）。
