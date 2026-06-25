<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { questionsApi, type Question } from '@/api/questions'
import { recordsApi, type RecordDto } from '@/api/records'
import QuestionCountPicker from '@/components/QuestionCountPicker.vue'
import QuestionCard from '@/components/QuestionCard.vue'

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

// 已记录态
const todayRecord = ref<RecordDto | null>(null)

const filledCount = computed(
  () => Object.values(answers.value).filter((v) => v && v.trim().length > 0).length,
)

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

// 编辑态下切 count 重新出题
watch(count, async () => {
  if (mode.value !== 'editing') return
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
  }
  mode.value = 'editing'
}

async function onSubmit() {
  if (filledCount.value === 0) {
    errorMsg.value = '至少回答一题，留空的会自动跳过'
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
          :model-value="answers[q.id] ?? ''"
          @update:model-value="(v) => (answers[q.id] = v)"
        />
      </div>

      <p v-if="errorMsg" class="mt-3 text-sm text-red-500">{{ errorMsg }}</p>

      <button
        type="button"
        :disabled="submitting || filledCount === 0"
        class="mt-6 w-full rounded-2xl bg-mint-500 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-mint-600 disabled:bg-mint-300 disabled:opacity-70"
        @click="onSubmit"
      >
        {{ submitting ? '保存中…' : `保存今日记录（已填 ${filledCount}/${questions.length}）` }}
      </button>
    </template>
  </main>
</template>
