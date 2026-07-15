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

- 三个维度都可选：`time`（`morning` / `afternoon` / `evening` / `late_night`）、`day`（具体星期几 `monday`...`sunday`，或分组 `weekend` / `weekday`）、`season`（`spring` / `summer` / `autumn` / `winter`）
- 任一维度命中当下场景即算「匹配」（`OR` 语义，不要求同时满足多个维度）
- `tags` 为 `null` 或空 → 通用题，不参与场景匹配，只作为兜底池

**现状（2026-07-13 种子数据）**：题库共 73 道，`event`/`emotion`/`future` 三类基本均衡（26/23/24），其中 21 道带 `tags`（8 道老题后补的 + 13 道新写的场景题），其余 52 道是无标签通用题。

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

```
QuestionContext = { timeOfDay, dayOfWeek, dayGroup, season }   // 来自当下（或补写时拼出来的）LocalDateTime

池 A（matched） = 题库中 tags 命中 ctx 任一维度的题
池 B（general）  = 其余（含无 tags 的通用题）

seed = SHA-256(userId : date : count : ctx.timeOfDay) 取前 8 字节当 long
用 seed 分别 shuffle 池 A、池 B（同一输入永远同一 shuffle 结果）

HOOK_SIZE = 2   // 前几道优先给「钩子」（场景匹配题）
hookTarget = min(HOOK_SIZE, count)

1) 结果前 hookTarget 道优先从 A 取
2) A 不够补 hookTarget 用 general 补
3) 剩下名额从 general 取
4) general 也不够时，从 A 剩余兜底（题库很小或场景命中过多才会走到这一步）
```

**稳定性边界（跟原设计不同的地方）**：seed 里带了 `ctx.timeOfDay`，意味着**同一天不同时段刷新会换一套题**——这是有意为之，配合沉浸式问答流做「什么时候写、题就贴合什么时候」的体验；但 `day` / `season` 不计入 seed，因为它们变化慢、且已经隐含在 `date` 里了。

**不同 count 之间仍不保证子集关系**；**仍不做历史去重**，靠题库量 + 场景分桶保证不容易连续撞题。

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
- [x] 场景匹配 + 稳定随机 Service（`QuestionContext` + hash seed + hook/general 分池）
- [ ] 单测：目前主要靠 `record-service` 那批测试间接覆盖，`QuestionService` 本身还没有专门单测

### 前端
- [x] `QuestionCountPicker.vue`（1~7）
- [x] 沉浸式单题卡片（内联在 `HomeView.vue`，未拆成独立 `QuestionCard.vue` 组件文件）
- [x] API 封装 `api/questions.ts`（`today` + `backfill`）
- [x] 接入 `HomeView.vue`（见 [records.md](./records.md) 详述）

---

## 6. 相关模块

- 依赖 [auth](./auth.md) 拿 userId
- 被 [records](./records.md) 消费（用户答完 → 提交到 records；补写模式下 records 的 `BackfillPolicy` 同时约束这里的 `/backfill` 接口）
