<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { questionsApi, type Question } from '@/api/questions'
import { recordsApi, type ImageDto, type RecordDto } from '@/api/records'
import VoiceRecorder from '@/components/VoiceRecorder.vue'
import ImageUploader from '@/components/ImageUploader.vue'

type Mode = 'loading' | 'recorded' | 'answering' | 'finishing' | 'error'

const auth = useAuthStore()

const mode = ref<Mode>('loading')
const errorMsg = ref<string | null>(null)
const today = ref<string>('')

// answering 状态
const desiredCount = ref(5)
const countPickerOpen = ref(false)
const questions = ref<Question[]>([])
const answers = ref<Record<number, string>>({})
const currentIndex = ref(0)

// finishing 状态
const freeText = ref<string>('')
const voiceUrl = ref<string | null>(null)
const voiceDuration = ref<number | null>(null)
const images = ref<ImageDto[]>([])
const summaryOpen = ref(false)
const submitting = ref(false)

// 夜猫子模式：凌晨 < 5:00 时显示「这其实是昨天写的吗」勾选项
// 勾上 → recordDate 写成前一天
const isLateNightWrite = ref(false)              // 是否勾选「归到昨天」
const forceNightOwl = ref(false)                 // 测试钩子：强制显示提示（dev 验收用）
const showNightOwlPrompt = computed(() => {
  if (forceNightOwl.value) return true
  const h = new Date().getHours()
  return h < 5
})
const yesterdayLabel = computed(() => {
  // 直接从后端给的 today.value (Asia/Shanghai 当天日期) 减一天，
  // 避免用 new Date().toISOString() 在凌晨时段被 UTC 漂移坑（中国时区 +8）
  if (!today.value) return ''
  const parts = today.value.split('-').map(Number)
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return ''
  const [y, m, d] = parts as [number, number, number]
  const dt = new Date(y, m - 1, d)
  dt.setDate(dt.getDate() - 1)
  const yy = dt.getFullYear()
  const mm = (dt.getMonth() + 1).toString().padStart(2, '0')
  const dd = dt.getDate().toString().padStart(2, '0')
  return `${yy}-${mm}-${dd}`
})

const canSubmit = computed(
  () =>
    filledCount.value > 0 ||
    !!voiceUrl.value ||
    images.value.length > 0 ||
    freeText.value.trim().length > 0,
)

// 已答的列表（finishing 摘要用）
const answeredList = computed(() =>
  questions.value
    .map((q) => ({ q, text: (answers.value[q.id] ?? '').trim() }))
    .filter(({ text }) => text.length > 0),
)

// 已记录态
const todayRecord = ref<RecordDto | null>(null)

const currentQuestion = computed(() => questions.value[currentIndex.value])
const isFirst = computed(() => currentIndex.value === 0)
const isLast = computed(() => currentIndex.value === questions.value.length - 1)

const filledCount = computed(
  () => Object.values(answers.value).filter((v) => v && v.trim().length > 0).length,
)

const categoryLabel: Record<Question['category'], string> = {
  event: '事件',
  emotion: '情绪',
  future: '未来',
}

onMounted(loadToday)

async function loadToday() {
  mode.value = 'loading'
  try {
    const rec = await recordsApi.today()
    if (rec) {
      todayRecord.value = rec
      today.value = rec.recordDate
      mode.value = 'recorded'
    } else {
      await loadQuestions(desiredCount.value)
      mode.value = 'answering'
    }
  } catch (e) {
    errorMsg.value = '加载失败，稍后再试'
    mode.value = 'error'
  }
}

async function loadQuestions(n: number) {
  const res = await questionsApi.today(n)
  questions.value = res.questions
  today.value = res.date
  answers.value = Object.fromEntries(res.questions.map((q) => [q.id, '']))
  currentIndex.value = 0
}

async function changeCount(n: number) {
  if (n === desiredCount.value) {
    countPickerOpen.value = false
    return
  }
  desiredCount.value = n
  countPickerOpen.value = false
  await loadQuestions(n)
}

async function enterAnswering() {
  // 从「已记录」回去重写：预填已有的语音 / 图 / 自由文本；
  // 问答部分：有则回显，没有则按当前 desiredCount 抓新的
  if (todayRecord.value) {
    freeText.value = todayRecord.value.freeText ?? ''
    voiceUrl.value = todayRecord.value.voiceUrl
    voiceDuration.value = todayRecord.value.voiceDuration
    images.value = todayRecord.value.images.map((img) => ({ ...img }))
    const existing = todayRecord.value.answers
    if (existing.length > 0) {
      questions.value = existing.map((a) => ({
        id: a.questionId ?? -1,
        category: (a.category ?? 'event') as Question['category'],
        content: a.question,
      }))
      answers.value = Object.fromEntries(existing.map((a) => [a.questionId ?? -1, a.answer]))
      desiredCount.value = questions.value.length
      currentIndex.value = 0
    } else {
      await loadQuestions(desiredCount.value)
    }
  }
  mode.value = 'answering'
}

function getAnswer(id: number): string {
  return answers.value[id] ?? ''
}

function setAnswer(id: number, v: string) {
  answers.value[id] = v
}

function next() {
  if (isLast.value) {
    goFinish()
  } else {
    currentIndex.value++
  }
}

function prev() {
  if (!isFirst.value) currentIndex.value--
}

function skip() {
  if (currentQuestion.value) {
    answers.value[currentQuestion.value.id] = ''
  }
  next()
}

function goFinish() {
  mode.value = 'finishing'
}

function backToAnswering() {
  mode.value = 'answering'
}

function onVoiceUploaded(p: { voiceUrl: string; duration: number }) {
  voiceUrl.value = p.voiceUrl
  voiceDuration.value = p.duration
}

function onVoiceCleared() {
  voiceUrl.value = null
  voiceDuration.value = null
}

async function onSubmit() {
  submitting.value = true
  errorMsg.value = null
  try {
    // 夜猫子勾选时把 recordDate 改成昨天；否则按 today.value（来自 questions/today 后端返回的 date）
    const effectiveDate = isLateNightWrite.value ? yesterdayLabel.value : today.value
    const payload = {
      recordDate: effectiveDate,
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
      images: images.value.map((img) => ({
        url: img.url,
        width: img.width,
        height: img.height,
        bytes: img.bytes,
      })),
      freeText: freeText.value.trim() || null,
    }
    const saved = await recordsApi.save(payload)
    todayRecord.value = saved
    // 如果保存到了昨天，本会话里的 todayRecord 概念其实是「昨天的」，
    // 这里依然把 mode 切到 recorded 让用户看到自己写完的成果；
    // 用户下次进首页会按真实今天重新加载
    mode.value = 'recorded'
    isLateNightWrite.value = false
  } catch (e) {
    errorMsg.value = axios.isAxiosError(e) ? (e.response?.data?.message ?? '保存失败') : '保存失败'
  } finally {
    submitting.value = false
  }
}

// 切换 desiredCount 后题目变了，currentIndex 可能越界
watch(questions, () => {
  if (currentIndex.value >= questions.value.length) {
    currentIndex.value = Math.max(0, questions.value.length - 1)
  }
})
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <!-- 顶部：日期 + 昵称 + 导航 -->
    <header class="mb-8 flex items-end justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ today || '今天' }}</p>
        <h1 class="text-2xl font-bold text-mint-600">你好，{{ auth.nickname ?? '默刻用户' }}</h1>
      </div>
      <nav class="flex items-center gap-2 text-xs text-gray-500">
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
        <RouterLink to="/random" class="rounded-full px-3 py-1 hover:bg-mint-50">随机</RouterLink>
        <button class="rounded-full px-3 py-1 hover:bg-mint-50" @click="auth.logout(); $router.push('/login')">退出</button>
        <!-- DEV: 测试钩子，强制显示夜猫子勾选项；生产前删 -->
        <button
          class="rounded-full px-2 py-1 text-[10px] opacity-30 hover:opacity-100"
          :class="forceNightOwl ? 'bg-mint-500 text-white' : 'bg-gray-200'"
          @click="forceNightOwl = !forceNightOwl"
          title="DEV: 强制显示夜猫子勾选"
        >🌙</button>
      </nav>
    </header>

    <!-- loading -->
    <div v-if="mode === 'loading'" class="rounded-3xl bg-white p-6 text-center text-sm text-gray-400">
      加载中…
    </div>

    <!-- error -->
    <div v-else-if="mode === 'error'" class="rounded-3xl bg-red-50 p-6 text-center text-sm text-red-500">
      {{ errorMsg }}
    </div>

    <!-- 已记录 -->
    <template v-else-if="mode === 'recorded' && todayRecord">
      <p class="mb-4 text-sm text-mint-600">今天已经记录过啦 · 想改的话点底下「重新写」</p>
      <div class="space-y-3">
        <article
          v-for="(a, i) in todayRecord.answers"
          :key="a.id ?? i"
          class="rounded-3xl bg-white p-5 shadow-sm"
        >
          <header class="mb-2 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">{{ a.category ? categoryLabel[a.category as Question['category']] : '—' }}</span>
            <span class="text-gray-400">{{ a.question }}</span>
          </header>
          <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{{ a.answer }}</p>
        </article>
        <section v-if="todayRecord.freeText" class="rounded-3xl bg-white p-5 shadow-sm">
          <header class="mb-2 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">还想说</span>
          </header>
          <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{{ todayRecord.freeText }}</p>
        </section>
        <section v-if="todayRecord.voiceUrl" class="rounded-3xl bg-white p-5 shadow-sm">
          <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">语音</span>
          </header>
          <audio :src="todayRecord.voiceUrl" controls class="w-full" />
        </section>
        <section v-if="todayRecord.images.length" class="rounded-3xl bg-white p-5 shadow-sm">
          <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">图片</span>
          </header>
          <div class="grid grid-cols-3 gap-2">
            <img v-for="img in todayRecord.images" :key="img.id ?? img.url" :src="img.url" class="aspect-square w-full rounded-2xl object-cover" />
          </div>
        </section>
      </div>
      <button class="mt-6 w-full rounded-2xl bg-white py-3 text-sm font-medium text-mint-600 shadow-sm transition hover:bg-mint-50" @click="enterAnswering">
        重新写
      </button>
    </template>

    <!-- answering：一次一个 -->
    <template v-else-if="mode === 'answering'">
      <!-- 顶部：圆点进度 + 数量调节 -->
      <div class="mb-8 flex items-center justify-between">
        <div class="flex gap-1.5">
          <span
            v-for="(_, i) in questions"
            :key="i"
            class="h-2 w-2 rounded-full transition"
            :class="
              i < currentIndex
                ? 'bg-mint-400'
                : i === currentIndex
                ? 'bg-mint-600 ring-2 ring-mint-200'
                : 'bg-gray-200'
            "
          />
        </div>
        <div class="relative">
          <button
            class="text-xs text-gray-400 hover:text-mint-600"
            @click="countPickerOpen = !countPickerOpen"
          >
            {{ desiredCount }} · 调
          </button>
          <div
            v-if="countPickerOpen"
            class="absolute right-0 top-6 z-10 flex rounded-2xl bg-white p-1 shadow-md"
          >
            <button
              v-for="n in 7"
              :key="n"
              class="h-8 w-8 rounded-xl text-xs transition"
              :class="n === desiredCount ? 'bg-mint-100 text-mint-600' : 'text-gray-500 hover:bg-mint-50'"
              @click="changeCount(n)"
            >
              {{ n }}
            </button>
          </div>
        </div>
      </div>

      <article v-if="currentQuestion" class="rounded-3xl bg-white p-6 shadow-sm">
        <header class="mb-5">
          <span class="rounded-full bg-mint-100 px-3 py-1 text-xs text-mint-600">
            {{ categoryLabel[currentQuestion.category] }}
          </span>
        </header>
        <p class="mb-6 text-xl font-medium leading-relaxed text-gray-800">
          {{ currentQuestion.content }}
        </p>
        <textarea
          :value="getAnswer(currentQuestion.id)"
          rows="5"
          placeholder="想到什么就写什么，留空就跳过"
          class="min-h-[140px] w-full resize-none rounded-2xl border border-mint-200 bg-mint-50 p-4 text-base leading-relaxed text-gray-800 placeholder:text-mint-400 outline-none transition focus:border-mint-500 focus:bg-white focus:ring-2 focus:ring-mint-100"
          @input="(e) => setAnswer(currentQuestion!.id, (e.target as HTMLTextAreaElement).value)"
        />
      </article>

      <!-- 底部操作 -->
      <nav class="mt-6 flex items-center justify-between">
        <button
          class="rounded-2xl px-4 py-2 text-sm text-gray-500 transition hover:bg-mint-50 disabled:opacity-30"
          :disabled="isFirst"
          @click="prev"
        >
          上一个
        </button>
        <button
          class="rounded-2xl px-4 py-2 text-sm text-gray-500 transition hover:bg-mint-50"
          @click="skip"
        >
          跳过
        </button>
        <div class="flex items-center gap-2">
          <button
            class="rounded-2xl bg-mint-500 px-6 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
            @click="next"
          >
            {{ isLast ? '完成' : '下一个' }}
          </button>
          <button
            v-if="!isLast"
            class="text-xs text-gray-400 underline-offset-2 hover:text-mint-600 hover:underline"
            @click="goFinish"
          >
            就到这里
          </button>
        </div>
      </nav>
    </template>

    <!-- finishing: 自由记录 + 语音 + 图片 + 保存 -->
    <template v-else-if="mode === 'finishing'">
      <button
        class="mb-6 text-xs text-gray-400 transition hover:text-mint-600"
        @click="backToAnswering"
      >
        ← 返回继续答
      </button>

      <!-- 折叠摘要 -->
      <section class="mb-3">
        <button
          class="flex w-full items-center justify-between rounded-3xl bg-white px-5 py-3 text-sm text-gray-600 shadow-sm transition hover:bg-mint-50"
          @click="summaryOpen = !summaryOpen"
        >
          <span>
            今天写了 <span class="font-medium text-mint-600">{{ filledCount }}</span> 个
            <span v-if="filledCount === 0" class="text-gray-400"> · 还没动笔也没关系</span>
          </span>
          <span class="text-xs text-gray-400">{{ summaryOpen ? '收起' : '展开' }}</span>
        </button>
        <div v-if="summaryOpen && answeredList.length" class="mt-2 space-y-2">
          <article
            v-for="(item, i) in answeredList"
            :key="i"
            class="rounded-2xl bg-mint-50/60 p-3"
          >
            <p class="mb-1 text-xs text-mint-600">
              <span class="rounded-full bg-mint-100 px-2 py-0.5">{{ categoryLabel[item.q.category] }}</span>
              <span class="ml-2 text-gray-400">{{ item.q.content }}</span>
            </p>
            <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-700">{{ item.text }}</p>
          </article>
        </div>
      </section>

      <!-- 我还想说 -->
      <section class="mb-3 rounded-3xl bg-white p-5 shadow-sm">
        <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
          <span class="rounded-full bg-mint-100 px-2 py-0.5">我还想说</span>
          <span class="text-gray-400">想到什么都行，不用很长</span>
        </header>
        <textarea
          v-model="freeText"
          rows="5"
          placeholder="今天还想说点什么？"
          class="min-h-[140px] w-full resize-none rounded-2xl border border-mint-200 bg-mint-50 p-4 text-base leading-relaxed text-gray-800 placeholder:text-mint-400 outline-none transition focus:border-mint-500 focus:bg-white focus:ring-2 focus:ring-mint-100"
        />
      </section>

      <!-- 语音 + 图片 -->
      <div class="space-y-3">
        <VoiceRecorder
          :initial-url="voiceUrl"
          :initial-duration="voiceDuration"
          @uploaded="onVoiceUploaded"
          @cleared="onVoiceCleared"
        />
        <ImageUploader v-model="images" />
      </div>

      <!-- 夜猫子勾选：凌晨 < 5:00 出现，可勾上将日记归到昨天 -->
      <label
        v-if="showNightOwlPrompt"
        class="mt-4 flex cursor-pointer items-center gap-3 rounded-2xl bg-mint-50/60 px-4 py-3 text-sm text-gray-600 transition hover:bg-mint-50"
      >
        <input
          v-model="isLateNightWrite"
          type="checkbox"
          class="h-4 w-4 accent-mint-500"
        />
        <span class="flex-1">凌晨也在写昨天的事？归到 {{ yesterdayLabel }}</span>
      </label>

      <p v-if="errorMsg" class="mt-3 text-sm text-red-500">{{ errorMsg }}</p>

      <button
        type="button"
        :disabled="submitting || !canSubmit"
        class="mt-6 w-full rounded-2xl bg-mint-500 py-3 text-base font-medium text-white shadow-sm transition hover:bg-mint-600 disabled:bg-mint-300 disabled:opacity-70"
        @click="onSubmit"
      >
        {{ submitting ? '保存中…' : '保存今日记录' }}
      </button>
    </template>
  </main>
</template>
