<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { questionsApi, type Question } from '@/api/questions'
import { recordsApi, type RecordDto } from '@/api/records'
import QuestionCountPicker from '@/components/QuestionCountPicker.vue'
import QuestionCard from '@/components/QuestionCard.vue'
import VoiceRecorder from '@/components/VoiceRecorder.vue'

type Mode = 'loading' | 'recorded' | 'editing' | 'error'

const auth = useAuthStore()

const mode = ref<Mode>('loading')
const errorMsg = ref<string | null>(null)
const today = ref<string>('')

// 编辑态
const count = ref(3)
const questions = ref<Question[]>([])
const answers = ref<Record<number, string>>({})
const submitting = ref(false)

// 编辑态的语音（已上传后的 url + duration）
const voiceUrl = ref<string | null>(null)
const voiceDuration = ref<number | null>(null)

// 已记录态
const todayRecord = ref<RecordDto | null>(null)

const filledCount = computed(
  () => Object.values(answers.value).filter((v) => v && v.trim().length > 0).length,
)

const canSubmit = computed(() => filledCount.value > 0 || !!voiceUrl.value)

onMounted(async () => {
  await loadToday()
})

async function loadToday() {
  mode.value = 'loading'
  try {
    const rec = await recordsApi.today()
    if (rec) {
      todayRecord.value = rec
      today.value = rec.recordDate
      mode.value = 'recorded'
    } else {
      await loadQuestions()
      mode.value = 'editing'
    }
  } catch (e) {
    errorMsg.value = '加载失败，稍后再试'
    mode.value = 'error'
  }
}

async function loadQuestions() {
  const res = await questionsApi.today(count.value)
  questions.value = res.questions
  today.value = res.date
  // 切 count 时清空已填；预初始化每题为空串，方便 v-model
  answers.value = Object.fromEntries(res.questions.map((q) => [q.id, '']))
}

// 编辑态下切 count 重新出题；count 与当前题数相同则跳过
// （enterEdit 里会程序化设置 count，需避免误触发把预填答案冲掉）
watch(count, async (newCount) => {
  if (mode.value !== 'editing') return
  if (newCount === questions.value.length) return
  await loadQuestions()
})

function enterEdit() {
  // 从「已记录」回到编辑，把已有答案预填
  if (todayRecord.value) {
    questions.value = todayRecord.value.answers.map((a) => ({
      id: a.questionId ?? -1,
      category: (a.category ?? 'event') as Question['category'],
      content: a.question,
    }))
    answers.value = Object.fromEntries(
      todayRecord.value.answers.map((a) => [a.questionId ?? -1, a.answer]),
    )
    count.value = questions.value.length || 3
    voiceUrl.value = todayRecord.value.voiceUrl
    voiceDuration.value = todayRecord.value.voiceDuration
  }
  mode.value = 'editing'
}

function getAnswer(id: number): string {
  return answers.value[id] ?? ''
}

function setAnswer(id: number, v: string) {
  answers.value[id] = v
}

function onVoiceUploaded(payload: { voiceUrl: string; duration: number }) {
  voiceUrl.value = payload.voiceUrl
  voiceDuration.value = payload.duration
}

function onVoiceCleared() {
  voiceUrl.value = null
  voiceDuration.value = null
}

async function onSubmit() {
  if (!canSubmit.value) {
    errorMsg.value = '至少答一题，或者录一段语音'
    return
  }
  errorMsg.value = null
  submitting.value = true
  try {
    const payload = {
      recordDate: today.value,
      answers: questions.value
        .map((q) => ({ q, text: (answers.value[q.id] ?? '').trim() }))
        .filter(({ text }) => text.length > 0)
        .map(({ q, text }) => ({
          questionId: q.id,
          question: q.content,
          category: q.category,
          answer: text,
        })),
      voiceUrl: voiceUrl.value,
      voiceDuration: voiceDuration.value,
    }
    const saved = await recordsApi.save(payload)
    todayRecord.value = saved
    mode.value = 'recorded'
  } catch (e) {
    if (axios.isAxiosError(e)) {
      errorMsg.value = e.response?.data?.message ?? '保存失败'
    } else {
      errorMsg.value = '保存失败'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <header class="mb-6 flex items-end justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ today || '今天' }}</p>
        <h1 class="text-2xl font-bold text-mint-600">你好，{{ auth.nickname ?? '默刻用户' }}</h1>
      </div>
      <nav class="flex gap-2 text-xs text-gray-500">
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
        <RouterLink to="/random" class="rounded-full px-3 py-1 hover:bg-mint-50">随机</RouterLink>
        <button class="rounded-full px-3 py-1 hover:bg-mint-50" @click="auth.logout(); $router.push('/login')">退出</button>
      </nav>
    </header>

    <!-- loading -->
    <div v-if="mode === 'loading'" class="rounded-3xl bg-white p-6 text-center text-sm text-gray-400">
      加载中…
    </div>

    <!-- error（首屏） -->
    <div v-else-if="mode === 'error'" class="rounded-3xl bg-red-50 p-6 text-center text-sm text-red-500">
      {{ errorMsg }}
    </div>

    <!-- 已记录 -->
    <template v-else-if="mode === 'recorded' && todayRecord">
      <p class="mb-3 text-sm text-mint-600">今天已经记录过啦 · 想改的话点底下「重新写」</p>
      <div class="space-y-3">
        <QuestionCard
          v-for="(a, i) in todayRecord.answers"
          :key="a.id ?? i"
          :question="{ id: a.questionId ?? -1, category: (a.category ?? 'event') as Question['category'], content: a.question }"
          :index="i"
          :model-value="a.answer"
          readonly
        />
        <section v-if="todayRecord.voiceUrl" class="rounded-3xl bg-white p-5 shadow-sm">
          <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">语音</span>
          </header>
          <audio :src="todayRecord.voiceUrl" controls class="w-full" />
        </section>
      </div>
      <button
        class="mt-6 w-full rounded-2xl bg-white py-3 text-sm font-medium text-mint-600 shadow-sm transition hover:bg-mint-50"
        @click="enterEdit"
      >
        重新写
      </button>
    </template>

    <!-- 编辑 -->
    <template v-else>
      <section class="mb-4 flex items-center justify-between">
        <span class="text-sm text-gray-600">今天答几题</span>
        <QuestionCountPicker v-model="count" />
      </section>

      <div class="space-y-3">
        <QuestionCard
          v-for="(q, i) in questions"
          :key="q.id"
          :question="q"
          :index="i"
          :model-value="getAnswer(q.id)"
          @update:model-value="(v: string) => setAnswer(q.id, v)"
        />

        <VoiceRecorder
          :initial-url="voiceUrl"
          :initial-duration="voiceDuration"
          @uploaded="onVoiceUploaded"
          @cleared="onVoiceCleared"
        />
      </div>

      <p v-if="errorMsg" class="mt-3 text-sm text-red-500">{{ errorMsg }}</p>

      <button
        type="button"
        :disabled="submitting || !canSubmit"
        class="mt-6 w-full rounded-2xl bg-mint-500 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-mint-600 disabled:bg-mint-300 disabled:opacity-70"
        @click="onSubmit"
      >
        {{ submitting ? '保存中…' : `保存今日记录（${filledCount} 题${voiceUrl ? ' + 语音' : ''}）` }}
      </button>
    </template>
  </main>
</template>
