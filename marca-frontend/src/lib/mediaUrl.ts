// 后端返回的图片/语音字段是相对路径（如 "/uploads/image/xxx.jpg"）。
// 网页版靠"前端和 /uploads 同源"能直接用，Capacitor App 里没有同源
// （页面 origin 是 https://localhost，不是 marca.xinliucc.cn），
// 相对路径必须拼上 VITE_API_BASE 才能解析到真实后端。
export function resolveMediaUrl(url: string | null | undefined): string | null {
  if (!url) return null
  if (/^https?:\/\//.test(url)) return url
  const base = import.meta.env.VITE_API_BASE ?? ''
  return base + url
}
