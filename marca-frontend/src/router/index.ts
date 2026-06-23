import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 业务路由在 Phase 2 各模块文档里实现
    // /login   → features/auth.md
    // /        → features/records.md (HomeView)
    // /timeline → features/records.md
    // /random   → features/records.md
  ],
})

export default router
