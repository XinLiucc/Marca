# 模块：问题推荐（Questions）

## 1. 概述

每天给用户推荐 N 道问题作为「话头」。问题数量由用户自由选择（默认 3，建议 1-5），同一天同一 count 的推荐结果稳定（不会刷新页面就换题）。

**职责边界：**
- 本模块只负责「出题」
- 用户答了什么、答了几题，由 [records 模块](./records.md) 负责

---

## 2. 数据库

### 表：`question`

```sql
CREATE TABLE question (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  category   ENUM('event', 'emotion', 'future') NOT NULL,  -- 维度标签
  content    VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT NOW()
);
```

**说明：**
- `category` 仅作打标和分布参考，**不强制**每天三个维度都覆盖
- 题库写死在 SQL 种子数据里，每个 category 至少 20 条，足够「同一题不会一周内反复出现」

### 种子数据示例

```sql
INSERT INTO question (category, content) VALUES
('event',   '今天有没有让你印象深刻的一件小事？'),
('event',   '今天和谁说了话？说了什么让你记住？'),
('emotion', '此刻的你，感觉像什么？'),
('emotion', '今天有没有哪个瞬间，让你想停一停？'),
('future',  '三年后的自己，会记住今天什么？'),
('future',  '如果今天可以留一句话给明天，是什么？');
-- ... 至少各 20 条
```

---

## 3. 后端接口

### 3.1 获取今日推荐问题

`GET /api/questions/today?count={n}`

**参数：**
- `count` — 题目数量，1-5，默认 3

**响应：**
```json
{
  "date": "2026-06-23",
  "questions": [
    { "id": 3,  "category": "event",   "content": "今天有没有让你印象深刻的一件小事？" },
    { "id": 11, "category": "emotion", "content": "此刻的你，感觉像什么？" },
    { "id": 7,  "category": "future",  "content": "三年后的自己，会记住今天什么？" }
  ]
}
```

**关键逻辑：**

```
seed = hash(userId + date + count)
按 category 均匀分桶（count=3 时各取 1，count=5 时 event/emotion/future = 2/2/1 等）
每桶用 seed 做稳定随机
```

- **稳定性**：同一用户、同一天、同一 count → 永远返回相同问题（避免刷新刷出新题）
- **不同 count 之间不保证子集关系**：count=5 不是 count=3 + 2，因为分桶策略不同。这是有意为之，让「换数量」感觉像换一套新题
- **不去重历史**：MVP 阶段不做「最近一周不重复」，靠题库量保证

---

## 4. 前端

### 4.1 组件

#### `QuestionCountPicker.vue`
- 顶部一行 chip：`1  2  3  4  5`
- 当前选中高亮
- 默认 3
- 选择后 emit `change`

#### `QuestionCard.vue`
- 单题展示：问题文本 + 文本框 + 「跳过这题」按钮
- props: `question`, `value`, `skipped`
- emits: `update:value`, `update:skipped`

### 4.2 状态

不需要单独 store。首页 `HomeView.vue` 内部用 `ref` / `reactive` 维护：

```ts
const count = ref(3);
const questions = ref<Question[]>([]);   // 接口返回
const answers = ref<Map<number, string>>(new Map());
const skipped = ref<Set<number>>(new Set());
```

### 4.3 接入流程

```
进入首页
  → 查 records/today（见 records 模块）
  → 如果今日已记录 → 展示已记录
  → 如果未记录 → 拉 questions/today?count=3 → 渲染卡片
  → 用户改 count → 重新拉
```

---

## 5. 开发清单

### 后端
- [ ] Question 实体 + Repository
- [ ] 种子 SQL 文件（每 category ≥ 20 题）
- [ ] `GET /api/questions/today?count=n` Controller
- [ ] 稳定随机 Service（hash 算法 + 分桶）
- [ ] 单测：同 user+date+count 多次调用结果一致

### 前端
- [ ] `QuestionCountPicker.vue`
- [ ] `QuestionCard.vue`
- [ ] API 封装 `api/questions.ts`
- [ ] 接入 `HomeView.vue`（见 records 模块详述）

---

## 6. 相关模块

- 依赖 [auth](./auth.md) 拿 userId
- 被 [records](./records.md) 消费（用户答完 → 提交到 records）
