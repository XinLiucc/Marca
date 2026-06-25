import axios from 'axios'

// 开发期由 vite.config.ts 的 proxy 转发到 http://localhost:8080
// 生产期通过 VITE_API_BASE 注入
const baseURL = import.meta.env.VITE_API_BASE ?? ''

export const http = axios.create({
  baseURL,
  timeout: 15000,
})

// 请求拦截器：附带 JWT
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('marca_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：401 → 清登录态 + 跳登录页（带 redirect）
http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('marca_token')
      localStorage.removeItem('marca_nickname')
      const path = window.location.pathname
      if (path !== '/login') {
        const redirect = encodeURIComponent(path + window.location.search)
        // 用 window.location 避免 api → store → router → api 的循环依赖
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    return Promise.reject(err)
  },
)
