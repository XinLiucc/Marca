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

// 响应拦截器：401 统一清理 token
http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('marca_token')
      // 路由跳转留给各模块自己处理（避免循环依赖 router）
    }
    return Promise.reject(err)
  },
)
