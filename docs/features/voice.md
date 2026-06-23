# 模块：语音记录（Voice）

## 1. 概述

独立于问答的语音笔记：用户可以「不答任何题，只录一段语音」。一条 record 最多挂一段语音（覆盖式更新）。

**职责边界：**
- 本模块管「录、传、存、放」
- 不管业务上的「这段语音属于哪天」—— 由 [records 模块](./records.md) 通过 voice_url 字段持有

---

## 2. 数据库

不新增表。复用 `record` 表的两个字段（见 [records.md](./records.md)）：

```sql
voice_url      VARCHAR(500),  -- 文件相对路径
voice_duration INT,           -- 秒
```

---

## 3. 文件存储

### MVP 方案：本地磁盘

```
{应用根目录}/uploads/voice/{yyyy-MM-dd}/{userId}_{timestamp}.{ext}
```

例如：`/uploads/voice/2026-06-23/1_1718000000.m4a`

- 通过 Spring Boot 配置静态资源映射：`/uploads/**` → 实际目录
- `voice_url` 数据库里存相对路径 `/uploads/voice/2026-06-23/1_xxx.m4a`
- 前端拼上 baseURL 即可播放

### 后续可演进

- 七牛云 / 阿里云 OSS / S3 — 只需要替换 Storage Service 实现，接口签名不变

---

## 4. 后端接口

### 4.1 上传语音

`POST /api/records/voice`

**请求**：`multipart/form-data`
- field `file`: 语音文件（建议格式 `.m4a` / `.webm`，浏览器和移动端都能播）
- 限制：大小 ≤ 10MB，时长 ≤ 5 分钟（前端先校验，后端兜底）

**响应：**
```json
{
  "voiceUrl": "/uploads/voice/2026-06-23/1_1718000000.m4a",
  "duration": 38
}
```

**服务端行为：**
- 落盘到约定路径
- 用 ffmpeg / jaudiotagger 读取时长（或前端传，后端信任）
- 返回 url + duration，由前端在「保存记录」时一并提交给 `POST /api/records`

> 上传和入库分两步的原因：用户可能录了不满意又重录，这段时间内不应该污染 record 表。真正落库时再关联。

### 4.2 替换 / 删除

- **替换**：再调一次上传 + 保存记录即可（覆盖 voice_url）
- **删除**：保存记录时 `voiceUrl: null` 即可（旧文件由清理任务处理，MVP 阶段不实现）

---

## 5. 前端

### 5.1 组件：`VoiceRecorder.vue`

**UI 状态机：**

```
[idle]  ─按下录音→  [recording]  ─停止→  [preview]
   ↑                                          │
   └──────── 重录 ←────────────────────────────┘
                                              │
                                          [uploading]
                                              │
                                         emit('uploaded', { url, duration })
```

**Props / Emits：**

```ts
defineProps<{
  initialUrl?: string;       // 编辑模式：回显已有语音
  initialDuration?: number;
}>();

defineEmits<{
  uploaded: [{ url: string; duration: number }];
  cleared:  [];              // 用户清掉了语音
}>();
```

**视觉：**
- 大圆形麦克风按钮 + 录音中波形动画
- 录完显示：试听播放器 + 时长 + 「重录」「删除」按钮
- 错误提示：麦克风权限、超长、上传失败

### 5.2 平台实现

#### Web 端：`MediaRecorder` API

```ts
const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
recorder.ondataavailable = (e) => chunks.push(e.data);
recorder.onstop = () => {
  const blob = new Blob(chunks, { type: 'audio/webm' });
  // 上传
};
```

#### 移动端：Capacitor 麦克风插件

- 推荐 `capacitor-voice-recorder` 或类似插件
- 抽出统一的 `composables/useRecorder.ts`，内部根据平台分发
- 接口对组件透明：`start()` / `stop(): Promise<Blob>` / `cancel()`

### 5.3 接入 HomeView

见 [records.md §4.2](./records.md#42-首页交互-homeviewvue)，VoiceRecorder 放在问题列表下方，独立组件，互不阻塞。

---

## 6. 开发清单

### 后端
- [ ] 配置 `/uploads` 静态资源映射 + 配置项 `marca.upload.dir`
- [ ] `POST /api/records/voice` Controller（multipart 上传）
- [ ] 文件大小 / MIME / 时长校验
- [ ] 文件命名 + 落盘 Service
- [ ] 单测：模拟上传 → 校验返回 url

### 前端
- [ ] `composables/useRecorder.ts`（Web 实现）
- [ ] `VoiceRecorder.vue` 组件 + 状态机
- [ ] 麦克风权限请求 + 错误兜底
- [ ] 试听播放器
- [ ] 接入 HomeView
- [ ] RecordCard full 模式下的语音播放器

### Phase 4（Capacitor）
- [ ] 接入 `capacitor-voice-recorder`（或同类插件）
- [ ] `useRecorder` 增加移动端分支
- [ ] iOS / Android 权限配置（Info.plist / AndroidManifest）

---

## 7. 相关模块

- 依赖 [auth](./auth.md)（上传接口需鉴权拿 userId 决定落盘路径）
- 写入 [records](./records.md) 的 `voice_url` / `voice_duration`
