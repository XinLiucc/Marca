# Marca · 默刻 项目计划书

> 完整或残缺，都是我们真实活过的证明。

---

## 1. 项目概述

**核心理念**：用最低的成本，留下今天的痕迹。
- 用「几个问题」打开话头，用户每天**自由选择回答几题**（1 道也行）
- 用「一段语音」承接说不出来的部分，独立于问答存在
- 用「一张 AI 插画」给今天一个画面感（**待确定**，后续阶段评估）

**名字含义**：
- *Marca* — 西班牙语「印记」，致敬聂鲁达的语言
- *默刻* — 默默地、悄悄地刻下，契合零压力的产品理念

**第一阶段目标**：核心记录功能跑通，30 天后还愿意继续打开它。

---

## 2. 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue3 + TypeScript + Vite | 主框架 |
| 样式 | Tailwind CSS + Nunito 字体 | 轻量，薄荷绿可爱风 |
| 后端 | Spring Boot（单体架构） | 熟悉的技术栈，轻量够用 |
| 数据库 | MySQL | 稳定，多用户场景适合 |
| 鉴权 | JWT | 无状态，Spring Boot 易集成 |
| 移动端 | Capacitor | Web 打包成 iOS / Android App |
| 部署 | 本地先跑，后期上服务器 | MVP 阶段不考虑部署 |

**架构决策：**
- 单体架构而非微服务：用户量初期有限，功能极简，微服务是过度设计
- 先做 Web 再打包 App：一套代码覆盖 Web + iOS + Android，Capacitor 麦克风插件后续语音输入直接复用

---

## 3. 数据结构设计

### 用户表：`user`

```sql
CREATE TABLE user (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  email      VARCHAR(255) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,   -- BCrypt 加密存储
  nickname   VARCHAR(50),
  created_at DATETIME DEFAULT NOW()
);
```

### 问题题库表：`question`

```sql
CREATE TABLE question (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  category   ENUM('event', 'emotion', 'future') NOT NULL,  -- 维度（仅作打标，不强制每天都覆盖）
  content    VARCHAR(255) NOT NULL,                         -- 问题内容
  created_at DATETIME DEFAULT NOW()
);
```

### 记录主表：`record`

一条 `record` = 某用户某天的一份记录。同一天的问答、语音、AI 插画都挂在同一条 `record` 下。

```sql
CREATE TABLE record (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,                  -- 归属用户
  record_date    DATE NOT NULL,                    -- 记录日期
  voice_url      VARCHAR(500),                     -- 当日语音文件路径（可空，独立于问答）
  voice_duration INT,                              -- 语音时长（秒）
  image_url      VARCHAR(500),                     -- AI 生成插画（待确定，可空）
  created_at     DATETIME DEFAULT NOW(),
  updated_at     DATETIME DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_user_date (user_id, record_date),  -- 每人每天只有一条
  FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### 问答明细表：`record_answer`

一条 `record` 下挂 0~N 条问答（用户当天答几题就插几条）。

```sql
CREATE TABLE record_answer (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id   BIGINT NOT NULL,                     -- 所属记录
  question_id BIGINT,                              -- 引用题库（可空，便于将来支持自定义问题）
  question    VARCHAR(255) NOT NULL,               -- 冗余存一份问题文本（题库改了也不影响历史）
  category    ENUM('event', 'emotion', 'future'),  -- 冗余维度，便于按维度统计
  answer      TEXT NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,              -- 同一条 record 内的展示顺序
  created_at  DATETIME DEFAULT NOW(),
  FOREIGN KEY (record_id)   REFERENCES record(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES question(id)
);
```

**设计要点：**
- 问题数量完全交给用户当天决定，DB 不再有「三问」假设
- 语音是独立字段，不依赖问答存在（可以只录一段语音、不答任何题）
- `image_url` 预留但待确定，落地前不强制依赖

---

## 4. 后端接口设计

### 4.1 用户认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT |

**POST `/api/auth/register` 请求体：**
```json
{
  "email": "hello@marca.app",
  "password": "your_password",
  "nickname": "默刻用户"
}
```

**POST `/api/auth/login` 响应：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "nickname": "默刻用户"
}
```

> 后续所有接口请求头均需携带：`Authorization: Bearer {token}`

### 4.2 问题相关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/questions/today?count={n}` | 获取今日推荐问题。`count` 由前端传入（用户当天自由选择，默认 3，建议 1-5），同一天内同一 count 返回结果稳定 |

**响应示例（count=3）：**
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

> 服务端只是**推荐**问题；用户可以全答、答一部分、或一道都不答。

### 4.3 记录相关

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/records` | 保存今日记录（问答 + 语音任意组合，至少一项非空） |
| POST | `/api/records/voice` | 上传/替换今日语音文件，返回 voice_url |
| GET | `/api/records` | 获取所有记录（时间轴用） |
| GET | `/api/records/today` | 获取今日记录 |
| GET | `/api/records/random` | 随机获取一条历史记录 |
| GET | `/api/records/{date}` | 按日期获取记录 |

**POST `/api/records` 请求体：**
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

**字段说明：**
- `answers` 是数组，长度由用户当天决定（0 也允许，只要有语音）
- `voiceUrl` / `voiceDuration` 可空，没录就不传
- `imageUrl` 暂不开放写入（待 AI 插画功能确定后再加）

**POST `/api/records/voice` 请求**：`multipart/form-data`，字段名 `file`，服务端落盘后返回 `{ voiceUrl, duration }`，前端再带着这两个值调 `POST /api/records` 完成入库。

---

## 5. 前端页面规划

### 5.1 页面列表

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录 / 注册 | `/login` | 邮箱 + 密码 |
| 今日记录 | `/` | 首页，问答 + 语音录制都在这里 |
| 时间轴 | `/timeline` | 按日期查看历史记录 |
| 随机回看 | `/random` | 随机展示过去某天 |

**首页交互要点：**
- 顶部一个 stepper / 下拉，让用户选「今天答几题」（默认 3，可改 1-5）
- 选定后调用 `/api/questions/today?count=n` 拉问题
- 每个问题卡片旁边有「跳过这题」按钮，用户可只答其中几道
- 页面底部一个语音录制区，独立于问答；只录语音也能提交

### 5.2 组件拆分（MVP 阶段从简）

```
src/
├── views/
│   ├── LoginView.vue       # 登录 / 注册
│   ├── HomeView.vue        # 今日记录（问答 + 语音）
│   ├── TimelineView.vue    # 时间轴
│   └── RandomView.vue      # 随机回看
├── components/
│   ├── QuestionCountPicker.vue  # 「今天答几题」选择器
│   ├── QuestionCard.vue         # 单个问题卡片（含跳过）
│   ├── VoiceRecorder.vue        # 语音录制 + 试听 + 上传
│   └── RecordCard.vue           # 历史记录卡片（展示问答 + 语音播放）
├── api/
│   └── index.ts            # 所有接口请求
├── stores/
│   └── auth.ts             # 用户登录状态（Pinia）
└── router/
    └── index.ts            # 路由配置（含登录守卫）
```

> 语音录制 Web 端用 `MediaRecorder` API 实现；移动端打包后由 Capacitor 麦克风插件接管，组件接口保持一致。

### 5.3 设计风格

- 配色：薄荷绿系（主色 `#1D9E75`，浅色 `#E1F5EE`）
- 字体：Nunito（圆润可爱）
- 风格：圆角 20px，卡通感，低压力
- 暂不引入 UI 组件库，手写 Tailwind CSS

---

## 6. 开发阶段规划

### Phase 0：准备（1 天）

- [ ] 创建 Spring Boot 项目（Maven）
- [ ] 创建 Vue3 + Vite + TypeScript 项目
- [ ] 配置 Tailwind CSS + Nunito 字体
- [ ] 建好数据库，执行建表 SQL
- [ ] 初始化问题题库数据

### Phase 1：后端核心（3～5 天）

- [ ] 实现注册 / 登录接口，JWT 鉴权
- [ ] 实现 `GET /api/questions/today?count=n`（按 count 随机出题，同日同 count 稳定）
- [ ] 实现 `POST /api/records` 保存记录（answers 数组 + 可选语音字段）
- [ ] 实现 `POST /api/records/voice` 语音文件上传
- [ ] 实现 `GET /api/records/today` 查今日记录
- [ ] 实现 `GET /api/records` 获取全部记录
- [ ] 实现 `GET /api/records/random` 随机回看
- [ ] 配置跨域（CORS）、文件存储目录

### Phase 2：前端对接（3～5 天）

- [ ] 搭路由框架（Vue Router + 登录守卫）
- [ ] 登录 / 注册页，JWT 存储（localStorage）
- [ ] 首页：选答题数 → 拉问题 → 答题（支持跳过）→ 录语音 → 提交
- [ ] 语音录制组件（Web MediaRecorder + 试听 + 上传）
- [ ] 时间轴页：展示历史记录列表（含语音播放）
- [ ] 随机回看页：点击随机展示一条历史记录
- [ ] 基础样式（能用就行，不追求完美）

### Phase 3：体验打磨（持续迭代）

- [ ] UI 细节优化（动画、过渡、薄荷绿风格完善）
- [ ] 今日已记录状态处理（支持补录 / 追加语音）
- [ ] 空状态处理（没有历史记录时的提示）
- [ ] 移动端适配（响应式布局）
- [ ] 语音波形展示 / 播放进度优化

### Phase 4：App 打包（Phase 3 完成后）

- [ ] 集成 Capacitor
- [ ] 接入 Capacitor 麦克风插件，替换 Web 录音实现
- [ ] 配置 iOS / Android 项目
- [ ] 真机测试

---

## 7. 后续功能规划（待评估）

| 功能 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| AI 每日插画 | **待确定** | 图像生成 API（如 DALL·E / SD） | 是否真的对用户有价值需要先验证；落地前 image_url 字段保留但不写入 |
| 语音转写 | 待评估 | 语音识别 API | 让语音也能在时间轴里以文本形式被检索 |
| 多设备同步 | 已天然支持 | 后端多用户结构 | 登录即同步 |
| PWA 离线支持 | 可选 | Vite PWA 插件 | 弱网体验 |

---

## 8. GitHub 仓库

**仓库名**：`marca` 或 `marca-app`

**Description**：
```
Marca · 默刻 — 每天三问，悄悄刻下一点今天。
```

**README 简介**：
```markdown
# Marca · 默刻

> 完整或残缺，都是我们真实活过的证明。

每天花 30 秒，回答几个小问题，或者只录一段语音，
为今天留下一点痕迹。不追求完整，只留值得被记住的一点点。

## Tech Stack

**Frontend** · Vue3 + TypeScript + Vite + Tailwind CSS
**Backend** · Spring Boot
**Database** · MySQL
**Mobile** · Capacitor（iOS / Android）

## Project Status

🌱 正在开发中 · MVP 阶段

## Inspired by

> "Puedo escribir los versos más tristes esta noche."
> — Pablo Neruda
```

---

## 9. 成功标准

> 30 天后，还愿意继续打开它。

---

## 10. Slogan

> 完整或残缺，都是我们真实活过的证明。
