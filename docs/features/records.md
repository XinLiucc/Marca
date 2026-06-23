# 模块：记录主流程（Records）

## 1. 概述

「一条 record = 某用户某天的一份记录」。问答、语音、AI 插画都挂在同一条 record 下。这是整个产品的核心模块。

**职责边界：**
- 本模块管「记录的存取」
- 题目内容由 [questions 模块](./questions.md) 提供
- 语音文件上传由 [voice 模块](./voice.md) 负责（本模块只持有最终的 url + duration）

---

## 2. 数据库

### 表：`record`（记录主表）

```sql
CREATE TABLE record (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  record_date    DATE NOT NULL,
  voice_url      VARCHAR(500),                     -- 见 voice 模块
  voice_duration INT,                              -- 秒
  image_url      VARCHAR(500),                     -- 见 ai-image 模块（待定）
  created_at     DATETIME DEFAULT NOW(),
  updated_at     DATETIME DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_user_date (user_id, record_date),
  FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### 表：`record_answer`（问答明细）

```sql
CREATE TABLE record_answer (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id   BIGINT NOT NULL,
  question_id BIGINT,                              -- 引用 question.id（可空，便于将来自定义题）
  question    VARCHAR(255) NOT NULL,               -- 冗余存一份文本（题库改了也不影响历史）
  category    ENUM('event', 'emotion', 'future'),  -- 冗余
  answer      TEXT NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT NOW(),
  FOREIGN KEY (record_id)   REFERENCES record(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES question(id)
);
```

**关键约束：**
- `(user_id, record_date)` 唯一 → 每人每天最多一条 record
- 一条 record 下可以有 0~N 条 answer（用户只录语音、一道题都不答也允许）
- 但「整条 record 必须至少有一项内容」（answer 非空 或 voice_url 非空）由 Service 层校验，DB 不强制

---

## 3. 后端接口

### 3.1 保存今日记录（创建或覆盖）

`POST /api/records`

**请求体：**
```json
{
  "recordDate": "2026-06-23",
  "answers": [
    {
      "questionId": 3,
      "question": "今天有没有让你印象深刻的一件小事？",
      "category": "event",
      "answer": "下午的咖啡特别好喝"
    },
    {
      "questionId": 7,
      "question": "三年后的自己，会记住今天什么？",
      "category": "future",
      "answer": "今天决定开始做 Marca"
    }
  ],
  "voiceUrl": "/uploads/voice/2026-06-23/uid_1.m4a",
  "voiceDuration": 38
}
```

**响应：** 返回完整的 record 对象（含 answers 数组）。

**Service 行为：**
- 先按 `(userId, recordDate)` 查 record
- 存在 → 更新（answers 全删重插，简单可靠）
- 不存在 → 新建
- 校验：`answers` 非空 或 `voiceUrl` 非空，否则 400

### 3.2 获取今日记录

`GET /api/records/today`

**响应：**
- 有记录：返回 record 对象
- 无记录：返回 `null` 或 204

### 3.3 获取所有记录（时间轴）

`GET /api/records?page=0&size=20`

**响应：**
```json
{
  "total": 87,
  "page": 0,
  "size": 20,
  "items": [
    { "id": 88, "recordDate": "2026-06-23", "answers": [...], "voiceUrl": "...", ... },
    ...
  ]
}
```

按 `record_date DESC` 排序。

### 3.4 按日期获取

`GET /api/records/{date}` — `date` 格式 `YYYY-MM-DD`

### 3.5 随机回看

`GET /api/records/random`

返回该用户的一条随机历史记录（排除今天）。如无历史，返回 `null` / 204。

---

## 4. 前端

### 4.1 页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 今日记录 | `/` | 首页：问答 + 语音录制，组合提交 |
| 时间轴 | `/timeline` | 历史记录列表 |
| 随机回看 | `/random` | 随机展示一条历史记录 |

### 4.2 首页交互（`HomeView.vue`）

```
进入首页
  ├─ 拉 GET /api/records/today
  │
  ├─ 已有记录
  │    展示「今日已记录」+ 内容回显 + 「编辑」按钮
  │
  └─ 无记录
       ├─ 顶部 QuestionCountPicker（默认 3）
       ├─ 拉 GET /api/questions/today?count=3 → 渲染 QuestionCard 列表
       ├─ 底部 VoiceRecorder（来自 voice 模块）
       └─ 「保存」按钮
           ├─ 收集已填答案（跳过的不传）
           ├─ 如果录了语音 → 先调 POST /api/records/voice 拿 url
           └─ 调 POST /api/records 落库
```

### 4.3 时间轴页（`TimelineView.vue`）

- 列表展示 `RecordCard`
- 支持下拉加载更多（分页）
- 每条卡片显示：日期、答题数、是否有语音/插画图标、点击展开详情

### 4.4 随机回看页（`RandomView.vue`）

- 一个大按钮「再随机一条」
- 点击 → 调 `/api/records/random` → 渲染 RecordCard 大图模式
- 显示「这是 X 天前的你」

### 4.5 组件

- `RecordCard.vue` — 历史记录卡片
  - props: `record`（含 answers / voice / image）
  - 模式：`compact`（时间轴用）/ `full`（详情/随机回看用）
  - full 模式下渲染语音播放器、AI 插画图（如有）

### 4.6 API 封装

`api/records.ts`：

```ts
saveRecord(payload)
getTodayRecord()
listRecords(page, size)
getRecordByDate(date)
getRandomRecord()
```

---

## 5. 开发清单

### 后端
- [ ] Record / RecordAnswer 实体 + Repository
- [ ] RecordService：upsert、列表、随机
- [ ] `POST /api/records`
- [ ] `GET /api/records/today`
- [ ] `GET /api/records?page=&size=`
- [ ] `GET /api/records/{date}`
- [ ] `GET /api/records/random`
- [ ] 单测：覆盖 upsert（新建 + 更新 + 全空校验）

### 前端
- [ ] `HomeView.vue`（最复杂的一个，建议最后写）
- [ ] `TimelineView.vue`
- [ ] `RandomView.vue`
- [ ] `RecordCard.vue`（compact + full 两种模式）
- [ ] `api/records.ts`
- [ ] 首页路由守卫 / 已记录状态处理

---

## 6. 相关模块

- 依赖 [auth](./auth.md)
- 消费 [questions](./questions.md)
- 与 [voice](./voice.md) 协作（语音 url 写入本表）
- 预留 [ai-image](./ai-image.md) 字段
