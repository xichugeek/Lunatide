# 06 · 周期预测引擎

> 位置：`domain/prediction/`（预测）和 `domain/stats/`（统计）。**纯 Kotlin，零 Android 依赖，纯函数，无副作用**。
> 这是 App 最核心的逻辑，必须有完整的单元测试（§8 列出的用例全部实现，可以更多）。

## 1. 接口

```kotlin
data class PredictionConfig(
    val manualCycleLength: Int?,     // 用户手动指定，优先级最高
    val manualPeriodLength: Int?,
    val initialCycleLength: Int?,    // 引导时填写，数据不足时作为先验
    val initialPeriodLength: Int?,
    val lutealPhaseDays: Int = 14,
    val futureCycles: Int = 6,
)

data class PredictionResult(
    val cycleLength: Int,               // 采用的周期长度
    val periodLength: Int,              // 采用的经期长度
    val confidence: Confidence,         // NONE / LOW / MEDIUM / HIGH
    val rangeDays: Int,                 // 预测误差范围 ±rangeDays
    val currentCycleStart: LocalDate?,  // 最近一次 <= today 的经期开始日
    val cycleDay: Int?,                 // 今天是本周期第几天（开始日=1）
    val inPeriod: Boolean,
    val nextPeriodStart: LocalDate?,    // 原始预测的下次开始日（不因推迟而移动）
    val daysLate: Int,                  // > 0 表示推迟天数
    val predictedPeriods: List<DateRange>,   // 未来预测经期（推迟时从今天起顺延）
    val fertileWindows: List<DateRange>,     // 未来预测易孕期（不显示时为空）
    val ovulationDays: List<LocalDate>,
    val fertilityHiddenReason: FertilityHiddenReason?, // DISABLED / IRREGULAR / OUT_OF_RANGE / NOT_ENOUGH_DATA / null
)

fun predict(periods: List<Period>, today: LocalDate, config: PredictionConfig): PredictionResult
fun computeStats(periods: List<Period>, dayLogs: List<DayLog>, today: LocalDate): CycleStats
```

## 2. 基础定义

- 将经期按 `start` 升序排列。
- **周期长度** `L_i = start_{i+1} − start_i`（天数）。最后一段经期之后的周期尚未完成，不计入。
- **经期长度** = `end − start + 1`，只统计已结束的经期。
- **有效结束日**（进行中经期）= `min(today, start + 14)`。

## 3. 周期长度 C 的计算

1. 若 `manualCycleLength != null` → `C = manualCycleLength`，置信度按数据量照常计算（用于范围显示）。
2. 取最近最多 12 个已完成周期长度。
3. **硬过滤**：去掉 `< 15` 或 `> 90` 的值（多半是漏记或误记）。
4. **离群过滤**：若剩余数量 ≥ 4，计算中位数 `m`，去掉 `|L − m| > 10` 的值（例如漏记一次导致的 56 天）。
5. 取过滤后最近 6 个值，记数量为 `n`：
   - `n == 0`：`C = initialCycleLength ?: 28`，置信度 `NONE`（没有任何经期记录）或 `LOW`（只有一次经期）。
   - `n in 1..2`：与先验混合 `C = round((sum(L) + prior) / (n + 1))`，`prior = initialCycleLength ?: 28`，置信度 `LOW`。
   - `n >= 3`：**加权平均**，从旧到新权重 1, 2, …, n：`C = round(Σ(w_i·L_i) / Σw_i)`。置信度：标准差 ≤ 2 → `HIGH`；≤ 4 → `MEDIUM`；否则 `LOW`。
6. 最终 `C` 限制在 `[15, 90]`。
7. 取整规则：四舍五入（`.5` 进位）。

## 4. 经期长度 P 的计算

- 手动指定优先。
- 取最近最多 6 次**已结束**经期的长度，过滤 `< 1` 或 `> 14` 的值。
- `n == 0` → `initialPeriodLength ?: 5`；`n in 1..2` → 与先验混合（同上）；`n >= 3` → 算术平均。
- 最终限制在 `[2, 10]`。

## 5. 预测

### 5.1 误差范围
- `n >= 3`：`rangeDays = clamp(round(标准差), 1, 7)`；
- 否则 `rangeDays = 3`。

### 5.2 下次经期
- `S` = 最近一次经期的开始日（无经期 → 所有预测字段为空，`confidence = NONE`）。
- `nextPeriodStart = S + C`。
- 若今天在进行中的经期内 → `inPeriod = true`，`daysLate = 0`。
- 若 `today > nextPeriodStart` 且没有更晚的经期 → `daysLate = today − nextPeriodStart`。

### 5.3 未来预测经期（用于日历和提醒）
- 锚点：`anchor = if (daysLate > 0) today else nextPeriodStart`。
- 第 k 个（k = 0 … futureCycles−1）：`[anchor + k·C, anchor + k·C + P − 1]`。
- 只保留**结束日 ≥ 今天**的区间；起始日早于今天的部分截掉（过去的日期不显示预测）。
- 若最近一次经期进行中，预测从 `S + C` 开始，不与当前经期重叠。

### 5.4 排卵日与易孕期
- 对每个预测经期开始日 `X`（包括 `nextPeriodStart`，即使当前已推迟也只用于计算后不显示过去日期）：
  - 排卵日 `O = X − lutealPhaseDays`
  - 易孕期 `[O − 5, O + 1]`
- 易孕期与任意经期（真实或预测）重叠的部分要裁掉。
- 只保留结束日 ≥ 今天的区间。
- **不显示**的情形（`fertileWindows` 为空并给出原因）：
  - 设置关闭 → `DISABLED`
  - `C < 21` 或 `C > 35` → `OUT_OF_RANGE`
  - 有效周期 `n >= 3` 且规律性为「不太规律」→ `IRREGULAR`
  - 没有任何经期记录 → `NOT_ENOUGH_DATA`
- 只要显示易孕期，UI 必须同时显示「仅供参考，不能用于避孕」。

## 6. 统计与规律性（`computeStats`）

```kotlin
data class CycleStats(
    val cycles: List<CycleSummary>,      // 每个周期：开始日、周期长度（未完成为 null）、经期长度（进行中为 null）
    val avgCycleLength: Double?,         // 使用 §3 过滤后的值做算术平均，显示保留整数
    val avgPeriodLength: Double?,
    val regularity: Regularity?,         // 有效周期 < 3 时为 null
    val topSymptoms: List<Pair<Symptom, Int>>,  // 最近 6 个周期，Top 5
    val painDistribution: Map<Int, Int>,
    val gentleTips: List<GentleTip>,
)
```

**规律性**（基于最近最多 6 个有效周期，`span = max − min`）：
- `span ≤ 9` → 规律（REGULAR）
- `10 ≤ span ≤ 16` → 较规律（SOMEWHAT_REGULAR）
- `span ≥ 17` → 不太规律（IRREGULAR）

**温和提示 GentleTip**（只做提示，不诊断；有效周期 ≥ 3 才判断）：
- 平均周期 < 21 或 > 35 → `CYCLE_OUT_OF_COMMON_RANGE`：「你的平均周期不在常见的 21–35 天范围内。每个人的情况不同，如果你有疑虑，可以咨询医生。」
- 平均经期 > 8 天 → `LONG_PERIODS`：「最近几次经期持续时间偏长，如果同时伴随经量很多或明显不适，可以咨询医生。」
- 规律性为 IRREGULAR → `IRREGULAR_CYCLES`：「最近几个周期长度变化较大。压力、作息、体重变化都可能有影响，如果持续这样，可以咨询医生。」
- 最近 3 次经期中 ≥ 2 次记录了「很痛」→ `SEVERE_PAIN`：「最近几次痛经比较明显，如果影响到日常生活，可以咨询医生。」

（文案放在字符串资源中，上面是参考。）

## 7. 性能

- 预测在 `Dispatchers.Default` 上计算；输入为内存中的列表，复杂度 O(n)，n ≤ 300 时应 < 5 ms。
- ViewModel 中对 `periods Flow + settings Flow + today` 做 `combine` 后计算，结果缓存为 `StateFlow`。
- “今天”在跨午夜时需要刷新（App 回到前台时重新获取今天日期即可）。

## 8. 必须实现的单元测试用例

固定 `today`，用构造的经期列表断言结果。日期写法 `D(2026,10,1)`。

| # | 场景 | 断言要点 |
|---|---|---|
| 1 | 没有任何记录 | 所有预测为空，confidence = NONE，fertility reason = NOT_ENOUGH_DATA |
| 2 | 仅一次经期，无先验 | C = 28，P = 5（若该经期已结束则用其长度与先验混合），next = start + 28，confidence = LOW |
| 3 | 仅一次经期，先验 C=32 | next = start + 32 |
| 4 | 两次经期，L=30，先验 28 | C = round((30+28)/2) = 29 |
| 5 | 6 个周期全是 28 | C = 28，range = 1，confidence = HIGH，regularity = REGULAR |
| 6 | 周期 [26,27,28,29,30,31]（旧→新） | 加权平均 = round(Σ w·L / 21) = 29（自行验证并写死期望值） |
| 7 | 含漏记离群值 [28,29,56,28,30] | 56 被离群过滤，C 基于 [28,29,28,30] |
| 8 | 含无效值 [10, 28, 100, 29] | 10 和 100 被硬过滤 |
| 9 | 手动指定 C=35 | 无论历史如何 C = 35 |
| 10 | 今天在进行中经期第 3 天 | inPeriod = true，cycleDay = 3，daysLate = 0 |
| 11 | 进行中经期已 20 天未结束 | 有效结束日 = start+14，不崩溃 |
| 12 | 今天 = next + 4，未记录新经期 | daysLate = 4，预测经期从今天开始顺延 |
| 13 | 今天 = next | daysLate = 0 |
| 14 | 易孕期计算，C = 28，next = 11-01 | O = 10-18，易孕期 [10-13, 10-19] |
| 15 | C = 40 | fertileWindows 为空，reason = OUT_OF_RANGE |
| 16 | 周期很不规律（span ≥ 17） | regularity = IRREGULAR，fertility reason = IRREGULAR |
| 17 | 设置关闭易孕期 | reason = DISABLED |
| 18 | 跨年：上次 2026-12-20，C=28 | next = 2027-01-17 |
| 19 | 闰年：2028-02-10 开始，C=28 | next = 2028-03-09 |
| 20 | 未来 6 个预测周期 | 列表长度 6（或因截断少于 6），相邻起始日相差 C，不与当前经期重叠 |
| 21 | 易孕期与预测经期重叠（C=21, P=7） | 重叠部分被裁掉 |
| 22 | 经期长度：已结束 [5,6,7]，进行中 1 次 | P = 6，进行中的不计入 |
| 23 | 统计：Top 症状 | 按次数降序，次数相同按 code 字典序，最多 5 个 |
| 24 | 温和提示：平均周期 38，≥3 个周期 | 包含 CYCLE_OUT_OF_COMMON_RANGE |
| 25 | 温和提示：只有 2 个周期且平均 38 | 不包含任何提示 |

同时为 `05_DATA_MODEL.md` §3 的经期编辑规则写用例（新建、相邻扩展、合并、删除首/尾/中间日、单日删除、进行中限制、未来日期拒绝等），每条规则至少一个测试。
