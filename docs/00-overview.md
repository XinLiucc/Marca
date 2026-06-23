# Marca · 默刻 项目总览

> 完整或残缺，都是我们真实活过的证明。

本文档是项目总览与索引。具体的数据库 / 接口 / 前端细节请进入 [features/](./features/) 各模块文档。

---

## 1. 项目定位

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
| 文件存储 | 本地磁盘（MVP）→ 后续可换对象存储 | 语音文件保存 |
| 移动端 | Capacitor | Web 打包成 iOS / Android App |

**架构决策：**
- 单体架构而非微服务：用户量初期有限，功能极简，微服务是过度设计
- 先做 Web 再打包 App：一套代码覆盖 Web + iOS + Android，Capacitor 麦克风插件后续语音录制直接复用

---

## 3. 设计原则

- **零压力** · 不做签到，不做连续打卡，不做提醒惩罚
- **极低成本** · 整个过程控制在 30 秒内
- **自由记录** · 想答几题答几题，想用语音用语音
- **长期主义** · 记录是为了未来回看，而不是即时反馈

---

## 4. 模块文档索引

| 模块 | 文档 | 内容 |
|------|------|------|
| 用户认证 | [features/auth.md](./features/auth.md) | 注册 / 登录 / JWT / auth store |
| 问题推荐 | [features/questions.md](./features/questions.md) | 题库 / 今日出题 / 数量选择 |
| 记录主流程 | [features/records.md](./features/records.md) | record + record_answer / 提交 / 时间轴 / 随机回看 |
| 语音 | [features/voice.md](./features/voice.md) | 录音 / 上传 / 回放 |
| AI 插画 | [features/ai-image.md](./features/ai-image.md) | **待确定**：生成时机 / API 候选 |

每个模块文档统一结构：
1. **概述**：要解决什么
2. **数据库**：相关表 / 字段
3. **后端接口**：路径 / 请求 / 响应
4. **前端**：页面 / 组件 / 状态
5. **开发清单**：可勾选的 TODO

---

## 5. 开发顺序

按依赖从底层往上推：

```
Phase 0 准备
   ↓
auth（必须先有用户）
   ↓
questions ──┐
            ├──→  records（问答主流程跑通）
            │
         voice（与 records 并行，但依赖 record 表已建好）
            ↓
        体验打磨 / Capacitor 打包
            ↓
        ai-image（待确定，不阻塞主流程）
```

### Phase 0：准备（1 天）

- [ ] 创建 Spring Boot 项目（Maven）
- [ ] 创建 Vue3 + Vite + TypeScript 项目
- [ ] 配置 Tailwind CSS + Nunito 字体
- [ ] 建好 MySQL 数据库 `marca`
- [ ] 配置 CORS、统一响应体、JWT 过滤器骨架
- [ ] 配置静态文件目录（后续语音存放）
- [ ] 仓库目录结构：
  ```
  marca/
  ├── marca-frontend/
  ├── marca-backend/
  ├── docs/
  └── README.md
  ```

### Phase 1：核心后端（按模块文档实现）

按 `auth → questions → records → voice` 顺序，每个模块文档里都有自己的「开发清单」。

### Phase 2：前端对接

同样按模块顺序，每个模块文档里有对应的页面/组件清单。

### Phase 3：体验打磨

- UI 细节、动画、薄荷绿风格
- 空状态、错误提示
- 移动端响应式

### Phase 4：App 打包

- 集成 Capacitor
- 麦克风插件替换 Web 录音
- 真机测试

### Phase 5：AI 插画（待确定）

见 [features/ai-image.md](./features/ai-image.md)。

---

## 6. 成功标准

> 30 天后，还愿意继续打开它。

---

## 7. Slogan

> 完整或残缺，都是我们真实活过的证明。
