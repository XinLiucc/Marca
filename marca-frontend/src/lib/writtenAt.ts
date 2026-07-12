// 跨日撰写：recordDate (日记日期) ≠ DATE(createdAt) (实际记录时间)。
// 覆盖两种场景：凌晨写昨天的事（差 1 天），以及补写更早之前的日子（差多天）。
// 日期不同就把 createdAt 的日期也带上，避免只显示时间让人猜不出是哪天写的。
export function writtenAtLabel(record: { recordDate: string; createdAt: string | null }): string | null {
  if (!record.createdAt) return null
  const createdDate = record.createdAt.slice(0, 10)
  if (createdDate === record.recordDate) return null

  const d = new Date(record.createdAt)
  const hh = d.getHours().toString().padStart(2, '0')
  const mm = d.getMinutes().toString().padStart(2, '0')
  const period = d.getHours() < 5 ? '凌晨 ' : ''
  return `记录于 ${createdDate} ${period}${hh}:${mm}`
}
