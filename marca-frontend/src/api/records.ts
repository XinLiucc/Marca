import { http } from './index'
import type { Category } from './questions'

export interface AnswerDto {
  id?: number
  questionId?: number | null
  question: string
  category?: Category | null
  answer: string
  sortOrder?: number
}

export interface RecordDto {
  id: number
  recordDate: string
  answers: AnswerDto[]
  voiceUrl: string | null
  voiceDuration: number | null
  imageUrl: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface SaveRecordPayload {
  recordDate: string
  answers: Omit<AnswerDto, 'id' | 'sortOrder'>[]
  voiceUrl?: string | null
  voiceDuration?: number | null
}

export interface RecordPage {
  total: number
  page: number
  size: number
  items: RecordDto[]
}

export const recordsApi = {
  save(payload: SaveRecordPayload) {
    return http.post<RecordDto>('/api/records', payload).then((r) => r.data)
  },
  today() {
    // 后端 204 时 axios.data 为空字符串 / undefined
    return http
      .get<RecordDto | ''>('/api/records/today')
      .then((r) => (r.status === 204 ? null : (r.data as RecordDto)))
  },
  list(page = 0, size = 20) {
    return http
      .get<RecordPage>('/api/records', { params: { page, size } })
      .then((r) => r.data)
  },
  random() {
    return http
      .get<RecordDto | ''>('/api/records/random')
      .then((r) => (r.status === 204 ? null : (r.data as RecordDto)))
  },
  byDate(date: string) {
    return http
      .get<RecordDto | ''>(`/api/records/${date}`)
      .then((r) => (r.status === 204 ? null : (r.data as RecordDto)))
  },
}
