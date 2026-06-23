# 模块：用户认证（Auth）

## 1. 概述

最基础的多用户支持：邮箱 + 密码注册登录，JWT 鉴权，后续所有业务接口都依赖这里。

**职责边界：**
- 本模块只管「我是谁」
- 不涉及业务数据（记录/语音都在各自模块）

---

## 2. 数据库

### 表：`user`

```sql
CREATE TABLE user (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  email      VARCHAR(255) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,         -- BCrypt 加密
  nickname   VARCHAR(50),
  created_at DATETIME DEFAULT NOW()
);
```

**说明：**
- 密码使用 BCrypt（Spring Security 自带），不要明文存
- 邮箱唯一索引，注册时校验
- 暂不实现邮箱验证 / 找回密码（MVP 阶段）

---

## 3. 后端接口

### 3.1 注册

`POST /api/auth/register`

**请求体：**
```json
{
  "email": "hello@marca.app",
  "password": "your_password",
  "nickname": "默刻用户"
}
```

**响应：**
```json
{
  "id": 1,
  "email": "hello@marca.app",
  "nickname": "默刻用户"
}
```

**校验：**
- email 格式合法、未被注册
- password 长度 ≥ 6
- nickname 可空，空时用邮箱前缀

### 3.2 登录

`POST /api/auth/login`

**请求体：**
```json
{
  "email": "hello@marca.app",
  "password": "your_password"
}
```

**响应：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "nickname": "默刻用户"
}
```

**说明：**
- token 过期时间建议 7 天（手机端友好）
- 错误统一返回 401，提示「邮箱或密码错误」（不区分以免泄露注册情况）

### 3.3 JWT 鉴权

- 后续所有 `/api/**`（除 `/api/auth/**`）请求头需带：`Authorization: Bearer {token}`
- 后端通过 Filter 解析 token，往 SecurityContext 注入 userId
- Controller 用 `@AuthenticationPrincipal` 或自定义注解拿当前 userId

---

## 4. 前端

### 4.1 页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录 / 注册 | `/login` | 同页面切换两种模式 |

### 4.2 组件

- `LoginView.vue` — 表单页，邮箱 + 密码（+ 注册时的昵称）
- 无独立子组件（表单逻辑简单）

### 4.3 状态管理

`stores/auth.ts`（Pinia）：

```ts
interface AuthState {
  token: string | null;
  nickname: string | null;
}

// actions
login(email, password)     // 调接口 → 存 token → 跳首页
register(email, pwd, nick) // 调接口 → 自动登录
logout()                   // 清 token → 跳登录页
```

### 4.4 持久化与路由守卫

- token 存 `localStorage`，刷新页面后自动恢复
- Axios 请求拦截器统一加 `Authorization` 头
- Axios 响应拦截器：401 自动 logout
- Vue Router beforeEach：未登录访问受保护路由 → 跳 `/login`

---

## 5. 开发清单

### 后端
- [ ] User 实体 + Repository
- [ ] BCrypt PasswordEncoder Bean
- [ ] JWT 工具类（签发 / 解析）
- [ ] JWT Filter + Spring Security 配置
- [ ] `POST /api/auth/register`
- [ ] `POST /api/auth/login`
- [ ] 统一异常处理（邮箱已存在 / 密码错误等）

### 前端
- [ ] `LoginView.vue` 表单 + 模式切换
- [ ] `stores/auth.ts` Pinia store
- [ ] Axios 实例 + 请求/响应拦截器
- [ ] Router 守卫
- [ ] localStorage 持久化

---

## 6. 相关模块

- 所有业务模块（[questions](./questions.md) / [records](./records.md) / [voice](./voice.md)）都依赖本模块的 JWT 鉴权
