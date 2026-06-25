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

export interface ImageDto {
  id?: number
  url: string
  width: number | null
  height: number | null
  bytes: number | null
  sortOrder?: number
}

export interface RecordDto {
  id: number
  recordDate: string
  answers: AnswerDto[]
  voiceUrl: string | null
  voiceDuration: number | null
  images: ImageDto[]
  createdAt: string | null
  updatedAt: string | null
}

export interface SaveRecordPayload {
  recordDate: string
  answers: Omit<AnswerDto, 'id' | 'sortOrder'>[]
  voiceUrl?: string | null
  voiceDuration?: number | null
  images?: Omit<ImageDto, 'id' | 'sortOrder'>[]
}

export interface RecordPage {
  total: number
  page: number
  size: number
  items: RecordDto[]
}

export interface VoiceUploadResponse {
  voiceUrl: string
  duration: number | null
  bytes: number
}

export interface ImageUploadResponse {
  imageUrl: string
  width: number | null
  height: number | null
  bytes: number
}

export const recordsApi = {
  save(payload: SaveRecordPayload) {
    return http.post<RecordDto>('/api/records', payload).then((r) => r.data)
  },
  uploadVoice(blob: Blob, duration: number) {
    const form = new FormData()
    const ext = blob.type.includes('webm') ? 'webm' : blob.type.includes('mp4') ? 'm4a' : 'audio'
    form.append('file', blob, `voice.${ext}`)
    return http
      .post<VoiceUploadResponse>('/api/records/voice', form, { params: { duration } })
      .then((r) => r.data)
  },
  uploadImage(file: File) {
    const form = new FormData()
    form.append('file', file, file.name)
    return http
      .post<ImageUploadResponse>('/api/records/image', form)
      .then((r) => r.data)
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
