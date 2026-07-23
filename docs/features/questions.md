# 模块：问题推荐（Questions）

## 1. 概述

每天给用户推荐 N 道问题作为「话头」。问题数量由用户自由选择（默认 5，1-7 之间），同一天同一 count（同一时段）的推荐结果稳定（不会刷新页面就换题）。

在原始设计（简单 hash 分桶）之上，实际落地加了一层**场景化出题（contextual）**：出题时会看当下的时段/周几/季节，优先把跟这个场景相关的题放在最前面，制造「这题怎么这么懂我此刻」的钩子效果，其余仍走稳定随机兜底。

**职责边界：**
- 本模块只负责「出题」
- 用户答了什么、答了几题，由 [records 模块](./records.md) 负责

---

## 2. 数据库

### 表：`question`

```sql
CREATE TABLE question (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  category   ENUM('event', 'emotion', 'future') NOT NULL,  -- 维度标签，仅统计参考，不强制覆盖
  content    VARCHAR(255) NOT NULL,
  tags       JSON NULL,                                    -- 场景标签，见下
  created_at DATETIME DEFAULT NOW()
);
```

**`tags` 字段（场景化出题用）：**

```json
{ "time": ["late_night"], "day": ["monday", "weekend"], "season": ["winter"] }
```

- 四个维度都可选：`time`（`morning` / `afternoon` / `evening` / `late_night`）、`day`（具体星期几 `monday`...`sunday`，或分组 `weekend` / `weekday`）、`season`（`spring` / `summer` / `autumn` / `winter`）、`holiday`（见下）
- `tags` 为 `null` 或空 → 通用题，不参与场景匹配，只作为兜底池

**`holiday` 取值**（`QuestionContext.holidayOf`，2026-07-23 新增）：
- 固定阳历日期：`new_year`(1/1) `valentines_day`(2/14) `womens_day`(3/8) `labor_day`(5/1)
  `childrens_day`(6/1) `teachers_day`(9/10) `national_day`(10/1) `singles_day`(11/11)
  `christmas_eve`(12/24) `christmas`(12/25)
- 农历日期（用 `hutool-core` 的 `ChineseDate` 换算，逐年阳历日期不同）：
  `new_year_eve`(除夕) `spring_festival`(春节) `lantern_festival`(元宵) `dragon_boat_festival`(端午)
  `mid_autumn_festival`(中秋)
- **`清明` 暂未支持**——它是节气不是固定农历日期，换算方式不一样，留到后面单独做

**现状（2026-07-23）**：题库共 79 道，`afternoon`/`winter` 场景题这一轮刚补过；题库扩充是持续任务，最新数量以 `seed.sql` 头部注释为准。

---

## 3. 后端接口

### 3.1 获取今日推荐问题

`GET /api/questions/today?count={n}`

**参数：**
- `count` — 题目数量，**1~7**，默认 5，超出范围 400 `INVALID_COUNT`

**响应：**
```json
{
  "date": "2026-06-23",
  "questions": [
    { "id": 3,  "category": "event",   "content": "今天吃了什么？哪一口让你停了一下？" },
    { "id": 11, "category": "emotion", "content": "此刻的你，感觉像什么？" },
    { "id": 7,  "category": "future",  "content": "三年后的自己，会记住今天什么？" }
  ]
}
```

### 3.2 补写过去某天的出题

`GET /api/questions/backfill?date={YYYY-MM-DD}&count={n}`

专给「补写忘记写的日子」用（见 [records.md](./records.md) §3.1 的补写窗口说明）。`date` 必须落在补写窗口内（`BackfillPolicy`），否则 400 `OUT_OF_BACKFILL_WINDOW`。

场景匹配的「时段」用**真实当下的钟点**、其余（周几/季节）用**目标日期**拼出场景上下文——周几/季节是那天客观发生的事实，可以用目标日期算；但时段没法伪造那天几点在写，只能用此刻真正的时钟。

### 3.3 出题算法（`QuestionService.pickDaily` / `QuestionContext`）

**2026-07-23 重构**：原算法把 time/day/season 三个维度命中的题混进同一个池子抢 `HOOK_SIZE=2` 个钩子名额——时段题库一扩大，季节/节日题被抽中的概率就被稀释，维度之间是「抢」而不是「分」的关系。改成按维度**占坑**，并加了**历史去重**：

```
QuestionContext = { timeOfDay, dayOfWeek, dayGroup, season, holiday }

时段池 = tags.time 命中 ctx.timeOfDay 的题        —— 固定占 1 个钩子名额
特殊池 = 节日 > 周几 > 季节，只取命中的最高一档   —— 占另 1 个钩子名额
        （节日题不空就只用节日题，没节日再看周几，都没有才落到季节；
         不会出现节日/周几/季节三个维度混着抢的情况）
通用池 = 其余（不在时段池、特殊池里的题，含无 tags 的通用题）

recentIds = 该用户最近 60 天（DEDUP_WINDOW_DAYS）内已经答过的 question_id
            （查 record_answer join record.record_date，不用额外建表）

seed = SHA-256(userId : date : count : ctx.timeOfDay) 取前 8 字节当 long
时段池 / 特殊池 / 通用池各自用 seed shuffle

HOOK_SIZE = 2；hookTarget = min(HOOK_SIZE, count)

1) 时段池取 1 道占第 1 个钩子名额
2) 特殊池取 1 道占第 2 个钩子名额
3) 上面某个池子是空的 → 用另一个池子顶替凑够 hookTarget
4) 还凑不够（题库很小）→ 通用池兜底
5) 剩下名额从通用池取
6) 通用池也不够 → 时段池/特殊池剩余兜底

每一步「取」的时候都先排除 recentIds；排除后候选不够这一步要取的数量，
就放弃去重直接用全部候选（相当于这一档题目抽完一轮后重新开始，
而不是卡死无题可出）。
```

**稳定性边界（跟原设计一致的地方）**：seed 里带了 `ctx.timeOfDay`，意味着**同一天不同时段刷新会换一套题**——配合沉浸式问答流做「什么时候写、题就贴合什么时候」的体验；`day` / `season` / `holiday` 不计入 seed，因为它们变化慢、且已经隐含在 `date` 里了。

**不同 count 之间仍不保证子集关系**。去重窗口是「最近答过」不是「从没出过」——只挡答过的题重复出现，不影响没被选中过的题的曝光。

---

## 4. 前端

### 4.1 组件

#### `QuestionCountPicker.vue`
- HomeView 内嵌的数字选择器（1~7），点击展开小面板选数字
- 当前选中高亮，选择后重新拉题

#### `QuestionCard.vue` / HomeView 内联问答卡片
- 沉浸式问答流里，每次只渲染**当前一题**（不是列表），详细交互见 [records.md](./records.md) 的「沉浸式问答流」小节
- 单题展示：分类标签 + 问题文本 + 文本框，留空即跳过

### 4.2 状态

不需要单独 store。`HomeView.vue` 内部用 `ref` 维护：

```ts
const desiredCount = ref(5)
const questions = ref<Question[]>([])
const answers = ref<Record<number, string>>({})
const currentIndex = ref(0)   // 当前正在答的第几题（沉浸式，一次一个）
```

### 4.3 接入流程

```
进入首页
  → 查 records/today（见 records 模块）
  → 今日已记录 → 跳转到 /record/:date 详情页回看（不再就地展示）
  → 未记录 → 拉 questions/today?count=5 → 沉浸式一次答一题
  → 用户改 count → 重新拉一整套（不保证子集关系）
  → 补写模式（backfill=date）→ 改拉 questions/backfill?date=&count=
```

---

## 5. 开发清单

### 后端
- [x] Question 实体 + Repository（含 `tags` JSON 字段）
- [x] 种子 SQL 文件（73 道，含场景标签）
- [x] `GET /api/questions/today?count=n`（1~7）
- [x] `GET /api/questions/backfill?date=&count=`
- [x] 场景匹配 + 稳定随机 Service（`QuestionContext` + hash seed + 时段/特殊/通用三池占坑分配）
- [x] 历史去重（`RecordAnswerRepository.findRecentQuestionIds`，60 天窗口，抽干净自动重置）
- [x] 节日维度（阳历固定日期 + `hutool-core` 农历换算，`清明` 暂不支持）
- [x] 单测：`QuestionContextTest`（节日日期换算）+ `QuestionServiceTest`（维度优先级 + 去重兜底）

### 前端
- [x] `QuestionCountPicker.vue`（1~7）
- [x] 沉浸式单题卡片（内联在 `HomeView.vue`，未拆成独立 `QuestionCard.vue` 组件文件）
- [x] API 封装 `api/questions.ts`（`today` + `backfill`）
- [x] 接入 `HomeView.vue`（见 [records.md](./records.md) 详述）

---

## 6. 相关模块

- 依赖 [auth](./auth.md) 拿 userId
- 被 [records](./records.md) 消费（用户答完 → 提交到 records；补写模式下 records 的 `BackfillPolicy` 同时约束这里的 `/backfill` 接口）
