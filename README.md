# Marca · 默刻

<p align="center">
  <br/>
  <em>完整或残缺，都是我们真实活过的证明。</em>
  <br/><br/>
</p>

> "Puedo escribir los versos más tristes esta noche."
> — Pablo Neruda

---

## 这是什么

没有记录日记的习惯。

不是因为不想，而是因为：

- 不知道记录什么
- 写日记成本太高
- 空白页面带来压力
- 难以长期坚持

**Marca · 默刻** 用「几个简单的问题」+「一段语音」，让记录变得简单自然。

每天花 30 秒，想答几题就答几题，想说话就直接说。  
不追求完整记录一天，只留值得被记住的一点点。

---

## 核心理念

**用最低的成本，留下今天的痕迹。**

- 不想写字？回答一两个问题就好
- 没有头绪？问题会替你打开话头
- 文字写不出？打开麦克风说一段
- 想给今天一张「画面」？让 AI 替你画出来（功能待定）

你永远是记录者，工具只是帮你开口。

---

## 功能

- 📝 **今日问答** — 每天随机推荐若干问题，自由选择回答几题（1 道也行）
- 🎙️ **语音记录** — 独立于问答的语音笔记，按下就录，留住声音里的今天
- 🗂 **时间轴** — 按日期查看所有历史记录
- 🎲 **随机回看** — 随机展示过去某一天，去年的今天、30 天前……
- 🎨 **AI 每日插画** — 根据当天记录生成一张配图（**待确定**）

---

## Tech Stack

| 层级 | 技术 |
|------|------|
| 前端 | Vue3 · TypeScript · Vite · Tailwind CSS |
| 后端 | Spring Boot |
| 数据库 | MySQL |
| 鉴权 | JWT |
| 移动端 | Capacitor（iOS / Android） |

---

## 项目结构

```
marca/
├── marca-frontend/     # Vue3 前端
│   ├── src/
│   │   ├── views/      # 页面
│   │   ├── components/ # 组件
│   │   ├── api/        # 接口请求
│   │   ├── stores/     # Pinia 状态管理
│   │   └── router/     # 路由
│   └── ...
└── marca-backend/      # Spring Boot 后端
    ├── src/main/java/
    │   └── app/marca/
    │       ├── controller/
    │       ├── service/
    │       ├── entity/
    │       └── repository/
    └── ...
```

---

## 快速开始

### 后端

```bash
cd marca-backend

# 配置数据库（application.yml）
# spring.datasource.url=jdbc:mysql://localhost:3306/marca

# 启动
./mvnw spring-boot:run
```

### 前端

```bash
cd marca-frontend

npm install
npm run dev
```

---

## 开发路线

- [x] 项目立项 · 技术选型
- [ ] 后端核心接口（注册登录 · 问答 · 记录）
- [ ] 前端页面对接
- [ ] UI 打磨（薄荷绿 · 圆角 · Nunito 字体）
- [ ] 语音录制 · 存储 · 回放
- [ ] Capacitor 移动端打包
- [ ] AI 每日插画（待确定）

---

## 设计原则

**零压力** · 不做签到，不做连续打卡，不做提醒惩罚

**极低成本** · 整个过程控制在 30 秒内

**自由记录** · 想答几题答几题，想用语音用语音

**长期主义** · 记录是为了未来回看，而不是即时反馈

---

## Inspired by

Pablo Neruda 的诗歌，以及那些想留住今天却不知从何开始的人。

---

<p align="center">
  <sub>🌿 正在开发中 · MVP 阶段</sub>
</p>
