// 补写窗口：只能补最近 WINDOW_DAYS 天忘记写的日子，跟后端
// BackfillPolicy.WINDOW_DAYS（marca-backend/.../service/BackfillPolicy.java）保持一致。
export const BACKFILL_WINDOW_DAYS = 3

export function daysAgo(dateStr: string): number {
  const [y, m, d] = dateStr.split('-').map(Number)
  const target = new Date(y ?? 1970, (m ?? 1) - 1, d ?? 1)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((today.getTime() - target.getTime()) / 86400000)
}

export function canBackfill(dateStr: string): boolean {
  const n = daysAgo(dateStr)
  return n > 0 && n <= BACKFILL_WINDOW_DAYS
}
