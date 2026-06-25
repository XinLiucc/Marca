import { http } from './index'

export type Category = 'event' | 'emotion' | 'future'

export interface Question {
  id: number
  category: Category
  content: string
}

export interface DailyQuestionsResponse {
  date: string // YYYY-MM-DD
  questions: Question[]
}

export const questionsApi = {
  today(count = 3) {
    return http
      .get<DailyQuestionsResponse>('/api/questions/today', { params: { count } })
      .then((r) => r.data)
  },
}
