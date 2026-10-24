# 05 · 数据模型与业务规则

## 1. 存储划分

| 数据 | 存储 | 是否加密 |
|---|---|---|
| 经期、每日记录、症状 | Room + SQLCipher（`lunatide.db`） | 是（SQLCipher） |
| 设置项、引导状态、提醒配置 | DataStore Preferences（`settings.preferences_pb`） | 否（不含健康数据） |
| PIN 哈希与盐 | DataStore（单独的 `security.preferences_pb`） | 否，但只存 PBKDF2 哈希 |
| 数据库密钥（被包裹后） | `noBackupFilesDir/db_key.bin` | 由 Android Keystore 的 AES-GCM 密钥包裹 |

> 注意：用户设定的「平均周期/经期长度」属于健康相关信息但非常粗粒度。为简单起见放 DataStore 可以接受；若你认为应放进加密库，在 `DECISIONS.md` 说明后实现。

## 2. 数据库表（Room Entity）

日期一律以 `epochDay: Long`（`LocalDate.toEpochDay()`）存储；时间戳用 `Long` 毫秒（仅用于 updatedAt，不参与业务）。

### 2.1 `periods`（一次经期）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | Long | PK autoGenerate | |
| `start_day` | Long | NOT NULL, UNIQUE, INDEX | 开始日 epochDay |
| `end_day` | Long? | NULL 表示进行中 | 结束日（含当天） |
| `created_at` | Long | NOT NULL | |
| `updated_at` | Long | NOT NULL | 合并导入时用于冲突判断 |

不变量（必须在 domain 层保证，并有单元测试）：
1. `end_day == null` 或 `end_day >= start_day`。
2. 任意两段经期**不重叠**，且之间至少间隔 1 天（相邻会被合并）。
3. 最多只有**一段**进行中的经期，且它必须是最晚开始的那一段。
4. `start_day <= today`；`end_day <= today`（不能记录未来）。
5. 进行中经期在显示与计算时的有效结束日 = `min(today, start_day + 14)`；超过 14 天仍未结束时，UI 提示用户确认结束日期（不自动改数据）。

### 2.2 `day_logs`（每日记录）

| 列 | 类型 | 说明 |
|---|---|---|
| `day` | Long | PK，epochDay |
| `flow` | String? | `SPOTTING` / `LIGHT` / `MEDIUM` / `HEAVY` / `VERY_HEAVY` |
| `pain` | Int? | 0–3（不痛/轻微/明显/很痛） |
| `mood` | String? | `HAPPY` / `CALM` / `LOW` / `IRRITABLE` / `ANXIOUS` / `SENSITIVE` |
| `bbt_centi_celsius` | Int? | 基础体温 ×100（例如 36.52℃ 存 3652），避免浮点误差 |
| `intercourse` | String? | `PROTECTED` / `UNPROTECTED` |
| `note` | String? | ≤ 500 字符 |
| `updated_at` | Long | |

### 2.3 `day_symptoms`（每日症状，多对多）

| 列 | 类型 | 说明 |
|---|---|---|
| `day` | Long | FK → `day_logs.day`，ON DELETE CASCADE |
| `symptom` | String | 症状 code（见 `04_FEATURE_SPEC.md` §4） |

主键 `(day, symptom)`。

> 枚举一律以**稳定的字符串 code** 存储（不用 ordinal），便于将来增加选项和备份兼容。未知 code（来自更新版本的备份）读取时忽略而非崩溃。

### 2.4 预留（v1.1，不在 v1.0 建表）
- `custom_symptoms`（自定义症状）。建表时通过 Room Migration 加入。

## 3. 经期编辑规则（domain/usecase，必须全部有单元测试）

### 3.1 记录「例假来了」（StartPeriod(date)）
- 若 date 已在某段经期内 → 无操作，返回提示「这天已经在经期里了」。
- 若存在进行中的经期且 date 在其开始日之后 → **不要**自动把旧经期结束（意图不明确）。返回错误「上一次经期还没结束，请先结束它」，UI 引导用户先结束。
- 若 date 与已有经期相邻（`date == P.end + 1` 或 `date == P.start - 1`）→ 视为同一次经期，扩展该经期，而不是新建。
- 否则新建经期 `[date, null]`（若 date < 今天且之后有更晚的经期，则新建为 `[date, date]` 并提示用户补充结束日——因为只有最晚的一段可以进行中）。

### 3.2 记录「例假走了」（EndPeriod(date)）
- 必须存在进行中的经期 P，且 `P.start <= date <= today`；设置 `P.end = date`。

### 3.3 日历中切换某天 d 的「在经期中」开关
**打开**：
- d 与经期 A 的结束相邻（`A.end + 1 == d`）且与经期 B 的开始相邻（`B.start - 1 == d`）→ 合并 A、B 为一段（`[A.start, B.end]`），删除 B。
- 仅与某段相邻 → 扩展那一段。
- 都不相邻 → 新建 `[d, d]`；若 d 是所有经期中最晚的且 d == today，则新建为进行中 `[d, null]`。
- 新建时若距离前一段经期结束 < 10 天 → 同 `04_FEATURE_SPEC.md` §2.3 的确认逻辑（可改为记录点滴出血）。

**关闭**（d 在经期 P 内）：
- P 只有一天 → 删除 P。
- d == P.start → `P.start = d + 1`。
- d == P.end（或 P 进行中且 d == today）→ `P.end = d - 1`。
- d 在中间 → 弹窗二选一：「这次经期在前一天就结束了」（`P.end = d - 1`，d 之后原属于 P 的日期不再是经期）/「取消」。不提供拆分为两段（太容易误操作）。
- 关闭后，被移出经期的日期上若有 `flow` 且不是 `SPOTTING`，一并清空 flow（确认框中说明）。

### 3.4 经量与经期的关系
- 在非经期日选择 LIGHT 及以上经量 → 提示「要把这天标为经期吗？」：是 → 执行 3.3 打开；否 → 取消选择。
- `SPOTTING` 可以出现在任何日期，不影响经期。

所有编辑都在**单个数据库事务**中完成。

## 4. 领域模型（domain/model）

```kotlin
data class Period(val id: Long, val start: LocalDate, val end: LocalDate?)   // end == null: 进行中
enum class Flow { SPOTTING, LIGHT, MEDIUM, HEAVY, VERY_HEAVY }
enum class Mood { HAPPY, CALM, LOW, IRRITABLE, ANXIOUS, SENSITIVE }
enum class Symptom { CRAMPS, BACK_PAIN, HEADACHE, /* ... */ }
enum class Intercourse { PROTECTED, UNPROTECTED }
data class DayLog(
    val day: LocalDate,
    val flow: Flow?, val pain: Int?, val mood: Mood?,
    val symptoms: Set<Symptom>,
    val bbtCelsius: BigDecimal?,      // 或以 Int 百分度表示，自行决定并记录
    val intercourse: Intercourse?,
    val note: String?,
)
```

Entity ↔ 领域模型的转换放在 `data/db/mappers`，UI 层不接触 Entity。

## 5. 设置项（DataStore 键）

| 键 | 类型 | 默认 |
|---|---|---|
| `onboarding_completed` | Boolean | false |
| `initial_cycle_length` | Int? | null（未知） |
| `initial_period_length` | Int? | null（未知） |
| `manual_cycle_length` | Int? | null（null=自动计算） |
| `manual_period_length` | Int? | null |
| `show_fertility` | Boolean | true |
| `show_bbt` | Boolean | true |
| `show_intercourse` | Boolean | false |
| `week_start` | String | `MONDAY` |
| `theme` | String | `SYSTEM` |
| `reminder_time_minutes` | Int | 1200（20:00） |
| `reminder_upcoming_enabled` / `_days_before` | Boolean / Int | true / 2 |
| `reminder_start_enabled` | Boolean | true |
| `reminder_end_enabled` | Boolean | true |
| `reminder_daily_enabled` | Boolean | false |
| `reminder_fertile_enabled` | Boolean | false |
| `notification_private_mode` | Boolean | true |
| `secure_screen` | Boolean | false（开启应用锁时自动置 true，用户可改） |
| `lock_enabled` / `lock_biometric` / `lock_timeout_seconds` | Boolean / Boolean / Int | false / false / 0 |

`security.preferences_pb`：`pin_hash`（Base64）、`pin_salt`（Base64）、`pin_iterations`（Int）、`failed_attempts`（Int）、`lockout_until_elapsed`（Long）。

## 6. 数据库版本与迁移

- v1.0 数据库 `version = 1`，开启 `exportSchema = true`，schema JSON 输出到 `app/schemas/` 并**提交到 Git**。
- 之后每次改表都写显式 `Migration` 并加迁移测试（`MigrationTestHelper`）；**禁止** `fallbackToDestructiveMigration()`（会丢用户数据，而用户没有云端副本）。
- 备份文件格式有独立的 `formatVersion`（见 `08_SECURITY_AND_BACKUP.md`），与数据库版本解耦。
