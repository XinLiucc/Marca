# 模块：记录主流程（Records）

## 1. 概述

「一条 record = 某用户某天的一份记录」。问答、语音、图片、自由文字、天气心情都挂在同一条 record 下。这是整个产品的核心模块，也是实际迭代中膨胀最多的一个——原始设计只有「问答 + 语音」，后续陆续加了：

- **图片上传**（`record_image` 子表，取代了最初设想的单张 AI 插画字段）
- **自由记录**（`free_text`，不挂在任何题目下的「我还想说」）
- **天气 / 心情**（`weather` 单选 + `moods` 多选，见下文「组件」小节的 `WeatherMoodPicker.vue`）
- **沉浸式问答流**（`HomeView` 从「列表全展开」改成「一次只答一题」）
- **独立详情页**（`/record/:date`，回看职责从首页彻底分离出去）
- **时间轴月历热图**（`MonthHeatmap`，`/api/records/month`）
- **补写 / 编辑 / 删除**（`BackfillPolicy` 统一窗口规则）

**职责边界：**
- 本模块管「记录的存取」
- 题目内容由 [questions 模块](./questions.md) 提供
- 语音文件上传由 [voice 模块](./voice.md) 负责（本模块只持有最终的 url + duration）
- 图片文件上传复用 voice 同一套 `StorageService`（本模块持有 url + 宽高 + 大小，见下）

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
  image_url      VARCHAR(500),                     -- 历史遗留字段，已被 record_image 子表取代，不再写入
  free_text      TEXT,                              -- 用户主动写的自由记录（"我还想说"），不挂任何题目
  weather        VARCHAR(16),                       -- 天气 key 单选，如 'sunny' / 'rainy'，null 表示未选
  moods          JSON,                               -- 心情 key 数组多选，如 ["happy","tired"]，null 表示未选
  created_at     DATETIME DEFAULT NOW(),
  updated_at     DATETIME DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_user_date (user_id, record_date),
  FOREIGN KEY (user_id) REFERENCES user(id)
);
```

> `image_url` 是最初为「AI 每日插画」设想预留的字段（见 [ai-image.md](./ai-image.md)），实际先落地的是用户手动上传图片，走的是全新的 `record_image` 子表，跟这个字段无关。`image_url` 目前一直是 `null`，代码里也不再读写它——只有 ai-image 那个远期构想真正启动时才会用到。

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

### 表：`record_image`（图片明细，一条 record 挂 0~N 张）

```sql
CREATE TABLE record_image (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id   BIGINT NOT NULL,
  url         VARCHAR(500) NOT NULL,
  width       INT,
  height      INT,
  bytes       INT,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT NOW(),
  FOREIGN KEY (record_id) REFERENCES record(id) ON DELETE CASCADE
);
```

**关键约束：**
- `(user_id, record_date)` 唯一 → 每人每天最多一条 record
- 一条 record 下 answer / image 都可以是 0~N 条
- 但「整条 record 必须至少有一项内容」由 Service 层校验，DB 不强制：`answers` 非空 **或** `voiceUrl` 非空 **或** `images` 非空 **或** `freeText` 非空
- `weather` / `moods` 都是可选项，不计入「是否有内容」的判断

---

## 3. 后端接口

### 3.1 保存记录（创建或覆盖，含今天 / 补写 / 编辑过去）

`POST /api/records`

**请求体：**
```json
{
  "recordDate": "2026-06-23",
  "answers": [
    {
      "questionId": 3,
      "question": "今天吃了什么？哪一口让你停了一下？",
      "category": "event",
      "answer": "下午的咖啡特别好喝"
    }
  ],
  "voiceUrl": "/uploads/voice/2026-06-23/uid_1.webm",
  "voiceDuration": 38,
  "images": [
    { "url": "/uploads/image/2026-06-23/uid_1.jpg", "width": 1080, "height": 1080, "bytes": 234567 }
  ],
  "freeText": "今天决定开始做 Marca",
  "weather": "sunny",
  "moods": ["happy", "tired"]
}
```

**响应：** 返回完整的 record 对象（含 answers / images 数组）。

**Service 行为（`RecordService.save`）：**
- `recordDate` 晚于今天 → 400 `FUTURE_DATE`（还没发生的日子写不了）
- `recordDate` 早于「补写窗口」→ 400 `OUT_OF_BACKFILL_WINDOW`（见下）
- 先按 `(userId, recordDate)` 查 record，存在则更新、不存在则新建
- `answers` / `images` 全删重插（简单可靠，避免三向 diff 合并）
- 校验：`answers` / `voiceUrl` / `images` / `freeText` 至少一项非空，否则 400 `EMPTY_RECORD`
- **显式刷新 `updatedAt`**：不能只靠 `@PreUpdate`，因为如果这次编辑只动了 answers/images 子表、Record 自身标量字段没变化，Hibernate 脏检查不会给 Record 本体发 UPDATE，`updatedAt` 刷不出来（曾经的隐藏 bug，现在 Service 里手动 `setUpdatedAt`）

**补写窗口（`BackfillPolicy`）：**

```java
WINDOW_DAYS = 3;
isWithinWindow(today, target) = !target.isAfter(today) && !target.isBefore(today.minusDays(3))
```

只能补写 / 编辑「最近 3 天内」的日子，不开放任意日期改写历史。这条规则同时约束 `POST /api/records` 和 [questions 模块的 `/api/questions/backfill`](./questions.md#32-补写过去某天的出题)，两边共用同一份逻辑。

### 3.2 获取今日记录

`GET /api/records/today` — 有记录返回 record 对象，无记录返回 204

### 3.3 获取所有记录（分页列表）

`GET /api/records?page=0&size=20`

```json
{ "total": 87, "page": 0, "size": 20, "items": [ { "id": 88, "recordDate": "2026-06-23", ... }, ... ] }
```

按 `record_date DESC` 排序。

### 3.4 按月获取（时间轴月历热图用）

`GET /api/records/month?year=2026&month=7`

返回该用户当月所有 record（不分页），前端拿来算哪些格子该点亮、哪些格子在补写窗口内。

### 3.5 按日期获取

`GET /api/records/{date}` — `date` 格式 `YYYY-MM-DD`，无记录返回 204

### 3.6 随机回看

`GET /api/records/random?exclude={date}`

返回该用户的一条随机历史记录。**永远排除「今天」**；`exclude` 是可选的额外排除项——比如正在详情页看某天时点"重逢往日"，把当前这天也排除掉，避免抽到自己。无历史时返回 204。

### 3.7 删除记录

`DELETE /api/records/{date}`

**不受补写窗口限制**——删除只是移除已写内容，不像补写/编辑那样会伪造「发生时间」，所以任何一天都能删（前端要求二次确认）。找不到记录返回 404 `RECORD_NOT_FOUND`。

### 3.8 上传语音 / 图片

`POST /api/records/voice`（multipart，field `file` + 可选 query `duration`）→ 返回 `{ voiceUrl, duration, bytes }`，详见 [voice 模块](./voice.md)。

`POST /api/records/image`（multipart，field `file`）→ 返回 `{ imageUrl, width, height, bytes }`。两者共用 `StorageService` 接口（`storeVoice` / `storeImage`），本地磁盘实现，MIME/大小校验，落盘路径按 `{类型}/{yyyy-MM-dd}/{userId}_{timestamp}.{ext}` 组织。宽高解析失败时 `width`/`height` 为 `null`，不影响保存。

上传和保存分两步：用户可能传了图又删、传了又不满意，这段时间不该污染 record 表；真正调 `POST /api/records` 时才把 url 列表一并落库。

---

## 4. 前端

### 4.1 页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 沉浸式问答流 | `/` | 写入专用：一次答一题 → 收尾（自由记录/语音/图片）→ 保存。也承担补写、编辑过去、今天重写 |
| 记录详情页 | `/record/:date` | 回看专用：展示某天完整内容 + 编辑/补写/删除/随机跳转入口 |
| 时间轴 | `/timeline` | 月历热图 + 当月记录列表 |
| 随机回看 | `/random` | 中转页：进来即抽一条 → 立即 replace 跳到该天的详情页 |
| 设置 | `/settings` | 见 [user-center.md](./user-center.md)，跟本模块无直接依赖 |

`HomeView` 现在**只管写入**，「回看今天的记录」这件事完全交给 `RecordDetailView`：进入首页时如果今天已经记录过、且 URL 没带 `?edit=1`，会直接 `router.replace` 跳到 `/record/:今天`，不会就地展示。

### 沉浸式问答流（`HomeView.vue`）

`mode` 是一个状态机：`loading → answering → finishing`（或 `error`）。

```
loading
  ├─ editDate=? （编辑过去某天，从详情页带过来）→ 拉该天 record → 预填 → 进 answering
  ├─ backfill=? （补写某个空日子，从详情页带过来）→ 拉一套新题 → 进 answering
  └─ 否则查 GET /api/records/today
       ├─ 有记录 且 没带 ?edit=1 → replace 到 /record/:date（不停留在首页）
       ├─ 有记录 且 带了 ?edit=1 → 预填已有内容 → 进 answering（「今天重写」入口）
       └─ 无记录 → 拉 GET /api/questions/today?count=desiredCount → 进 answering

answering（一次只渲染 currentQuestion 一题）
  ├─ 顶部 WeatherMoodPicker（天气单选 + 心情多选）
  ├─ 圆点进度条 + 数量调节（1~7，改动会重新整套拉题，不保证子集关系）
  ├─ 「上一个 / 跳过 / 下一个」，跳过=清空该题答案直接前进
  └─ 到最后一题点「完成」（或中途点「就到这里」）→ 进 finishing

finishing（收尾）
  ├─ 折叠摘要（可展开看已答的每一题）
  ├─ 「我还想说」自由文本框（free_text）
  ├─ VoiceRecorder（来自 voice 模块）
  ├─ ImageUploader（多图，即传即显示，删除只影响本地列表，保存时才落库）
  ├─ 夜猫子勾选（仅「今天」模式、凌晨 <5:00 时出现）：勾上把 recordDate 写成前一天
  └─ 「保存」→ POST /api/records → 保存成功 replace 到对应日期的详情页
```

**三种进入模式的区别只在于 `recordDate` 从哪来：**

| 模式 | 触发方式 | recordDate 来源 |
|------|----------|------------------|
| 今天正常写 / 重写 | 首页直接进 / 详情页「重新写」 | 后端 `questions/today` 返回的当天日期，凌晨可能被夜猫子勾选改成前一天 |
| 补写过去空的一天 | 详情页「补写这一天」→ `?backfill=date` | 固定为 query 里的 `date`，走 `questions/backfill` 出题，不出现夜猫子勾选 |
| 编辑过去已写的一天 | 详情页「编辑」→ `?editDate=date` | 固定为 query 里的 `date`，预填已有 answers/语音/图片/自由文本/天气心情 |

后两种统称「过去写模式」（`isPastWriteMode`），共享同一套 UI，只是有没有已有内容可预填的区别。补写/编辑都受 `BackfillPolicy` 的 3 天窗口限制（后端强制，前端 `lib/backfillWindow.ts` 同步一份规则提前判断要不要显示入口）。

**夜猫子模式**：判断 `new Date().getHours() < 5`，勾选后把 `recordDate` 改写成「今天」的前一天。算前一天时刻意避开 `Date.toISOString()`（会被 UTC 时区漂移坑，中国时区 +8），改成手工拆 `today`（后端返回的 Asia/Shanghai 当天日期字符串）再减一天。

### 记录详情页（`RecordDetailView.vue`，`/record/:date`）

- 拉 `GET /api/records/{date}`：有记录展示全部内容（问答、自由记录、语音、图片、天气心情标签、`writtenAtLabel` 时间提示）；无记录时按「是不是今天」「在不在补写窗口内」分别展示「去写今天的」/「补写这一天」/ 禁用态 + 文案
- 底部操作：今天 → 「重新写」；窗口内的过去某天 → 「编辑」；任意过去某天 → 「重逢往日」（随机跳到另一条历史，`exclude` 带上当前这天避免抽到自己）
- 「删除这一天」二次确认后 `DELETE /api/records/{date}`，成功后回时间轴

### 时间轴页（`TimelineView.vue`）+ 月历热图（`MonthHeatmap.vue`）

- 顶部 `MonthHeatmap`：`GET /api/records/month` 按月拉数据，格子按「有记录 / 补写窗口内可补 / 都不是」三态着色，今天有 ring 高亮，每个格子可点，跳到该天的 `/record/:date`（没记录且在窗口内也能点进去走补写）
- 下方 `RecordCard`（`mode="compact"`）列表，点击同样跳详情页
- 翻页只能查看到当月（`isCurrentMonth` 时下一页按钮禁用）

### 随机回看（`RandomView.vue`）

纯中转页：挂载即调 `/api/records/random`，抽到就 `replace` 到 `/record/:date`，抽不到显示「还没有历史记录可以随机」+ 回首页/时间轴的链接。真正的展示交给详情页，这个页面自己不渲染内容。

### 组件

- **`RecordCard.vue`** — `compact`（时间轴用，摘要 + 天气心情 emoji + 类型统计）/ `full`（详情页/RandomView 曾经用过的完整展开模式）两态
- **`WeatherMoodPicker.vue`** — 天气单选（再点一次取消）+ 心情多选（toggle，按预设顺序排序避免点击次序错乱），选项定义在 `lib/weatherMood.ts`（`WEATHERS` 8 项 / `MOODS` 10 项，DB 只存 key，前端映射 emoji + 中文）
- **`ImageUploader.vue`** — 九宫格网格，选图即调 `POST /api/records/image` 上传，`v-model` 是 `ImageDto[]`，删除只改本地数组，真正落库要等外层调 `POST /api/records`
- **`MonthHeatmap.vue`** — 见上

### lib 工具

- `lib/weatherMood.ts` — 天气/心情 key ↔ emoji/中文 映射
- `lib/backfillWindow.ts` — 前端版 `BackfillPolicy.isWithinWindow`，跟后端同一份 3 天窗口规则，用来提前决定要不要显示「补写/编辑」入口（真正的强制校验仍在后端）
- `lib/writtenAt.ts` — 统一的「写于 xxx」时间标签，用 `updatedAt`（编辑后能看出改过，不特意区分「当天写的」和「跨天改的」）

### API 封装 `api/records.ts`

```ts
save(payload)              // POST /api/records
uploadVoice(blob, duration)
uploadImage(file)
today()
list(page, size)
month(year, month)
random(excludeDate?)
byDate(date)
remove(date)                // DELETE /api/records/{date}
```

---

## 5. 开发清单

### 后端
- [x] Record / RecordAnswer / RecordImage 实体 + Repository
- [x] RecordService：upsert（含天气/心情/自由文本/图片）、列表、按月、随机、删除
- [x] `POST /api/records`（含 `FUTURE_DATE` / `OUT_OF_BACKFILL_WINDOW` / `EMPTY_RECORD` 校验）
- [x] `GET /api/records/today`
- [x] `GET /api/records?page=&size=`
- [x] `GET /api/records/month?year=&month=`
- [x] `GET /api/records/{date}`
- [x] `GET /api/records/random?exclude=`
- [x] `DELETE /api/records/{date}`
- [x] `POST /api/records/voice` / `POST /api/records/image`
- [x] `BackfillPolicy`（补写窗口，questions 模块共用）
- [x] 单测：`record-service` 那批（覆盖 upsert 新建/更新/全空校验），是项目第一批落地的单元测试

### 前端
- [x] `HomeView.vue`（沉浸式问答流，最复杂的一个）
- [x] `RecordDetailView.vue`
- [x] `TimelineView.vue` + `MonthHeatmap.vue`
- [x] `RandomView.vue`（中转页）
- [x] `RecordCard.vue`（compact + full）
- [x] `WeatherMoodPicker.vue` / `ImageUploader.vue`
- [x] `api/records.ts`
- [x] 首页「已记录自动跳详情页」逻辑

---

## 6. 相关模块

- 依赖 [auth](./auth.md)
- 消费 [questions](./questions.md)（含补写出题接口）
- 与 [voice](./voice.md) 协作（语音 url 写入本表，图片上传复用同一套 `StorageService`）
- `image_url` 字段预留给 [ai-image](./ai-image.md)（远期构想，暂不使用）
