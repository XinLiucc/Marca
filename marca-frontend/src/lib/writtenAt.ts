// 每条记录都标注完整的实际写作日期时间，不管 recordDate (日记日期) 是否等于
// DATE(createdAt)（当天写 / 凌晨写昨天的事 / 补写更早之前的日子，格式统一）。
// 用 updatedAt 而不是 createdAt：没编辑过时两者相等（@PrePersist 同时写入），
// 编辑过之后自然显示最新的编辑时间，不用另外加一条「最后编辑于」。
export function writtenAtLabel(record: {
  recordDate: string
  createdAt: string | null
  updatedAt: string | null
}): string | null {
  const at = record.updatedAt ?? record.createdAt
  if (!at) return null
  const atDate = at.slice(0, 10)

  const d = new Date(at)
  const hh = d.getHours().toString().padStart(2, '0')
  const mm = d.getMinutes().toString().padStart(2, '0')
  const period = d.getHours() < 5 ? '凌晨 ' : ''

  return `记录于 ${atDate} ${period}${hh}:${mm}`
}
