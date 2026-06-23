# 模块：AI 每日插画（AI Image）

> ⚠️ **状态：待确定**
>
> 这个功能是否真的有价值需要先验证。本文档记录当前的设想，但**不进入 MVP 实现范围**。
> 在 Phase 1~4 主流程跑通、有真实用户使用 30 天以上之前，本模块代码不开工。

---

## 1. 想解决什么

让用户除了文字 / 语音之外，还能拥有一张「今天的画面」，作为时间轴上的视觉锚点，回看时更容易被触动。

**待验证的假设：**
- AI 生成图是否真的能精准捕捉用户当天的感觉？还是流于装饰？
- 用户会不会觉得「AI 替我表达」违背了产品的零压力理念？
- 生成成本是否在可承受范围（免费用户能不能用）？

---

## 2. 数据库（已预留）

`record` 表已有字段（见 [records.md](./records.md)）：

```sql
image_url VARCHAR(500),  -- AI 生成插画
```

落地前**不写入**。如果最终决定不做，字段保留无害。

---

## 3. 候选方案

### 3.1 生成时机

| 方案 | 优点 | 缺点 |
|------|------|------|
| 用户保存记录时同步生成 | 即时看到 | 阻塞保存、生成失败处理麻烦 |
| 异步任务，几秒后推送 | 不阻塞 | 需要 WebSocket / 轮询 |
| 「夜间批量」生成当天所有记录 | 成本低 | 不即时，仪式感差一点 |

**倾向**：异步任务 + 轮询，结合「生成中」占位图。

### 3.2 Prompt 构造

输入信号：
- 当天所有 answer 文本
- 当天的天气 / 地点（如果有）
- 风格关键词（统一设定，如「水彩、温柔、低饱和」）

**示例 prompt 模板（草稿）：**
```
A soft watercolor illustration capturing the feeling of:
{answer_summary_in_english}.
Mood: gentle, introspective, low-saturation.
Style: minimal, hand-drawn, slight grain.
No text, no human face.
```

### 3.3 图像 API 候选

| API | 优点 | 缺点 |
|------|------|------|
| OpenAI DALL·E 3 | 质量稳、prompt 友好 | 单价较高 |
| Stable Diffusion（自部署） | 一次性投入，长期省钱 | 需要 GPU 服务器 |
| Replicate / 阿里云通义万相 / 字节豆包 | 国内可用、按量付费 | 风格各异需调研 |

国内部署优先考虑国内 API（合规 + 速度）。

---

## 4. 接口草稿（未实现）

```
POST /api/records/{id}/generate-image    # 触发生成
GET  /api/records/{id}/image-status      # 查询生成状态
```

返回的 image_url 落回 `record.image_url`。

---

## 5. 前端预留

- `RecordCard.vue` full 模式预留插画展示区
- 时间轴卡片可显示「有插画」图标
- 「重新生成」按钮（如果用户不满意）

---

## 6. 决策检查点

在动手实现前，至少回答清楚：

- [ ] MVP 上线满 30 天，有真实用户在用吗？
- [ ] 用户在反馈里有没有自然提到「想要图」？
- [ ] 单次生成成本估算 ≤ 多少？月活成本可控吗？
- [ ] 国内/海外合规怎么处理？
- [ ] 选定哪家 API？是否做过 prompt 调试 demo？

只有以上都有明确答案，才进入开发。否则**保持本字段为 NULL，本模块不动**。

---

## 7. 相关模块

- 字段挂在 [records](./records.md)
- 触发时机依赖 [records](./records.md) 的保存接口
