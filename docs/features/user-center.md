# 模块：用户中心（User Center）

## 1. 概述

2026-07-14 加的最小版用户中心：查看账号信息 + 改昵称 + 退出登录。原本挂在首页的「退出」按钮改成了「设置」入口，跳到独立的 `/settings` 页面。

**职责边界：**
- 本模块管「账号信息的查看与编辑」（目前只有昵称能改）
- 注册 / 登录 / JWT 鉴权仍属于 [auth 模块](./auth.md)，本模块的接口本身也需要登录态

**没做的（讨论过，先不做）：**
- 头像 — 私人日记没有社交面，用不上头像；以后如果要加，倾向 emoji / 预设头像，不做真实照片上传
- 改密码 — 需要旧密码校验，目前优先级不高

---

## 2. 数据库

不新增表，复用 [auth 模块](./auth.md) 的 `user` 表，只是多读了 `created_at`、多写了 `nickname`。

---

## 3. 后端接口

### 3.1 查看账号信息

`GET /api/auth/me`

**响应：**
```json
{
  "id": 2026071415304123841,
  "email": "hello@marca.app",
  "nickname": "默刻用户",
  "createdAt": "2026-06-25T15:52:41"
}
```

不含 `password`。需要登录态（`Authorization: Bearer {token}`），走跟其他业务接口一样的 JWT 过滤器。

### 3.2 改昵称

`PATCH /api/auth/me`

**请求体：**
```json
{ "nickname": "新昵称" }
```

**校验（`UpdateProfileRequest`）：** 非空、长度 ≤ 50 字，否则 400。

**响应：** 同 §3.1 的 `UserResponse`（更新后的完整账号信息）。

**说明：** `SecurityConfig` 不用为这两个接口额外开洞——只有 `POST /api/auth/**`（注册/登录）放行匿名访问，`GET`/`PATCH /api/auth/me` 自然落在「需要登录」的默认规则里。

---

## 4. 前端

### 4.1 页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 设置 | `/settings` | 账号信息（昵称行内编辑 / 邮箱 / 注册日期）+ 退出登录 |

首页（`HomeView.vue`）顶部导航的「退出」按钮已经换成「设置」，退出登录本身挪到了设置页底部。

### 4.2 组件：`SettingsView.vue`

- 挂载即拉 `GET /api/auth/me`
- 昵称是唯一可编辑项：点「编辑」→ 输入框 + 保存/取消，`@keyup.enter` 也能提交；本地先校验非空/≤50 字再提交，保存成功后同步 `auth.setNickname()`（Pinia store + localStorage 一起更新，首页问候语立刻跟着变）
- 邮箱、注册日期只读展示；`createdAt` 是后端给的 naive `"2026-06-25T15:52:41"` 字符串，前端直接拆字符串取年月日，不做时区换算（避免引入新的时区坑）
- 底部「退出登录」调 `auth.logout()` 后跳转 `/login`

### 4.3 API 封装 `api/auth.ts`

```ts
me()                          // GET /api/auth/me
updateProfile(nickname)       // PATCH /api/auth/me
```

---

## 5. 已知遗留

- **19 位用户 id 超过 JS 的 `Number.MAX_SAFE_INTEGER`**（id 是「14 位时间戳 + 5 位随机」的 BIGINT），JSON 解析到前端会丢精度。目前前端任何地方都不拿 id 当请求参数用，所以暂时无害；但以后如果要用 id 做请求参数（比如按 id 查/改用户），后端需要把 id 序列化成字符串，不能指望前端 JS number 保真。

---

## 6. 开发清单

### 后端
- [x] `GET /api/auth/me`
- [x] `PATCH /api/auth/me`（`UpdateProfileRequest` 校验）
- [x] `UserResponse` 补 `createdAt` 字段

### 前端
- [x] `SettingsView.vue`
- [x] `api/auth.ts` 补 `me` / `updateProfile`
- [x] 首页导航「退出」→「设置」入口
- [x] 改昵称后同步 `stores/auth.ts` + localStorage

---

## 7. 相关模块

- 依赖 [auth](./auth.md) 的 `user` 表和 JWT 鉴权
- 与其他业务模块（[questions](./questions.md) / [records](./records.md) / [voice](./voice.md)）无数据依赖，纯粹是账号自服务
