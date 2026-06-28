// 天气 / 心情的预设清单。DB 存 key，前端按 key 映射成 emoji + 中文。
// 想换 emoji 风格 / 加多语言 / 做"晴天的日记列表"统计，都在这里一处搞定。

export interface MetaOption {
  key: string
  emoji: string
  label: string
}

export const WEATHERS: readonly MetaOption[] = [
  { key: 'sunny',    emoji: '☀️', label: '晴' },
  { key: 'cloudy',   emoji: '☁️', label: '多云' },
  { key: 'overcast', emoji: '🌥', label: '阴' },
  { key: 'rainy',    emoji: '🌧️', label: '雨' },
  { key: 'thunder',  emoji: '⛈️', label: '雷' },
  { key: 'snow',     emoji: '❄️', label: '雪' },
  { key: 'fog',      emoji: '🌫️', label: '雾' },
  { key: 'windy',    emoji: '🌬️', label: '风' },
] as const

export const MOODS: readonly MetaOption[] = [
  { key: 'happy',    emoji: '😊', label: '开心' },
  { key: 'calm',     emoji: '😌', label: '平静' },
  { key: 'tired',    emoji: '😴', label: '累' },
  { key: 'sad',      emoji: '😔', label: '难过' },
  { key: 'anxious',  emoji: '😰', label: '焦虑' },
  { key: 'excited',  emoji: '✨', label: '兴奋' },
  { key: 'numb',     emoji: '😶', label: '麻木' },
  { key: 'lonely',   emoji: '🥺', label: '孤独' },
  { key: 'hopeful',  emoji: '🌱', label: '期待' },
  { key: 'angry',    emoji: '😠', label: '生气' },
] as const

const weatherMap = new Map(WEATHERS.map((w) => [w.key, w]))
const moodMap = new Map(MOODS.map((m) => [m.key, m]))

export function weatherOf(key: string | null | undefined): MetaOption | null {
  if (!key) return null
  return weatherMap.get(key) ?? null
}

export function moodOf(key: string | null | undefined): MetaOption | null {
  if (!key) return null
  return moodMap.get(key) ?? null
}

/** 把一个 mood key 数组映射成 MetaOption 数组（过滤掉未知 key） */
export function moodsOf(keys: string[] | null | undefined): MetaOption[] {
  if (!keys || !keys.length) return []
  return keys.map((k) => moodMap.get(k)).filter((m): m is MetaOption => !!m)
}
