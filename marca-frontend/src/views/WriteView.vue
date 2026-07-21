<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { questionsApi, type Question } from '@/api/questions'
import { recordsApi, type ImageDto, type RecordDto } from '@/api/records'
import VoiceRecorder from '@/components/VoiceRecorder.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import WeatherMoodPicker from '@/components/WeatherMoodPicker.vue'

// WriteView：只管写入，answering 一次一个问题 → finishing 收尾保存。
// 到达这个路由本身就代表"要写/要编辑"的意图，不用再靠 ?edit=1 这种旗标猜测：
// 没有 backfill/editDate 参数时，今天有记录就直接预填进编辑流，没有就出新题。
// 「回看」「今天状态摘要」职责在 HomeView (/)；「认真看某一天完整内容」在
// RecordDetailView (/record/:date)。写完普通今天的记录后 replace 回首页，
// 首页自己重新拉今天状态展示，不用带参数。
type Mode = 'loading' | 'answering' | 'finishing' | 'error'

const router = useRouter()
const route = useRoute()

const mode = ref<Mode>('loading')
const errorMsg = ref<string | null>(null)
const today = ref<string>('')

// 补写模式：从详情页「补写这一天」带 ?backfill=YYYY-MM-DD 过来（那天是空的，走全新出题）
const backfillDate = computed(() => {
  const q = route.query.backfill
  return typeof q === 'string' ? q : null
})
const isBackfillMode = computed(() => backfillDate.value !== null)

// 编辑过去模式：从详情页「编辑」带 ?editDate=YYYY-MM-DD 过来（那天已经写过，预填已有内容）
const editPastDate = computed(() => {
  const q = route.query.editDate
  return typeof q === 'string' ? q : null
})
const isEditPastMode = computed(() => editPastDate.value !== null)

// 补写 + 编辑过去，两种模式下 recordDate 都是固定的目标日期，不走今天/夜猫子那套逻辑
const isPastWriteMode = computed(() => isBackfillMode.value || isEditPastMode.value)
const pastTargetDate = computed(() => backfillDate.value ?? editPastDate.value)

// answering 状态
const desiredCount = ref(5)
const countPickerOpen = ref(false)
const questions = ref<Question[]>([])
const answers = ref<Record<number, string>>({})
const currentIndex = ref(0)

// 天气 / 心情（answering 顶部，单选 / 多选）
const weather = ref<string | null>(null)
const moods = ref<string[]>([])

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
const showNightOwlPrompt = computed(() => new Date().getHours() < 5)
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

onMounted(init)

async function init() {
  mode.value = 'loading'
  try {
    if (isEditPastMode.value) {
      const rec = await recordsApi.byDate(editPastDate.value!)
      if (!rec) {
        errorMsg.value = '这条记录好像不见了'
        mode.value = 'error'
        return
      }
      todayRecord.value = rec
      today.value = rec.recordDate
      await prefillFromRecord(rec)
      mode.value = 'answering'
      return
    }
    if (isBackfillMode.value) {
      // 补写目标日期在 RecordDetailView 已确认过是空的、且在窗口内，这里不用再查一遍
      await loadQuestions(desiredCount.value)
      mode.value = 'answering'
      return
    }
    const rec = await recordsApi.today()
    if (rec) {
      // 今天已有记录，到这个路由本身就是来编辑的 → 直接预填
      todayRecord.value = rec
      today.value = rec.recordDate
      await prefillFromRecord(rec)
      mode.value = 'answering'
    } else {
      await loadQuestions(desiredCount.value)
      mode.value = 'answering'
    }
  } catch (e) {
    errorMsg.value = axios.isAxiosError(e) ? (e.response?.data?.message ?? '加载失败，稍后再试') : '加载失败，稍后再试'
    mode.value = 'error'
  }
}

async function loadQuestions(n: number) {
  const res = isPastWriteMode.value
    ? await questionsApi.backfill(pastTargetDate.value!, n)
    : await questionsApi.today(n)
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

// 预填一条已有记录的语音 / 图 / 自由文本 / 天气 / 心情 / 问答；
// 问答部分：有则回显，没有则按当前 desiredCount 抓新的（今天重写、编辑过去某天都走这条路）
async function prefillFromRecord(rec: RecordDto) {
  freeText.value = rec.freeText ?? ''
  voiceUrl.value = rec.voiceUrl
  voiceDuration.value = rec.voiceDuration
  images.value = rec.images.map((img) => ({ ...img }))
  weather.value = rec.weather ?? null
  moods.value = rec.moods ? [...rec.moods] : []
  const existing = rec.answers
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
    // 夜猫子勾选时把 recordDate 改成昨天；补写模式下 today.value 就是目标日期；
    // 否则按 today.value（来自 questions/today 后端返回的 date）
    const effectiveDate =
      !isPastWriteMode.value && isLateNightWrite.value ? yesterdayLabel.value : today.value
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
      weather: weather.value,
      moods: moods.value,
    }
    const saved = await recordsApi.save(payload)
    isLateNightWrite.value = false
    if (isPastWriteMode.value) {
      // 补写 / 编辑过去某天，保存的不是"今天"，落地到那天的详情页才对
      router.replace({ name: 'record-detail', params: { date: saved.recordDate } })
    } else {
      // 正常写今天（含夜猫子归到昨天）：回首页，首页自己重新拉今天状态展示
      router.replace('/')
    }
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
    <!-- 顶部：日期 + 标题 + 导航 -->
    <header class="mb-8 flex items-end justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ today || '今天' }}</p>
        <h1 class="text-2xl font-bold text-mint-600">
          {{
            isBackfillMode
              ? '补写这一天'
              : isEditPastMode
                ? '编辑这一天'
                : '写点什么'
          }}
        </h1>
      </div>
      <nav class="flex items-center gap-2 text-xs text-gray-500">
        <RouterLink to="/" class="rounded-full px-3 py-1 hover:bg-mint-50">今日</RouterLink>
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
        <RouterLink to="/random" class="rounded-full px-3 py-1 hover:bg-mint-50">随机</RouterLink>
        <RouterLink to="/settings" class="rounded-full px-3 py-1 hover:bg-mint-50">设置</RouterLink>
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

    <!-- answering：一次一个 -->
    <template v-else-if="mode === 'answering'">
      <!-- 顶部：天气 / 心情 -->
      <div class="mb-4">
        <WeatherMoodPicker
          :weather="weather"
          :moods="moods"
          @update:weather="(v) => (weather = v)"
          @update:moods="(v) => (moods = v)"
        />
      </div>

      <!-- 圆点进度 + 数量调节 -->
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
        v-if="showNightOwlPrompt && !isPastWriteMode"
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
        {{
          submitting
            ? '保存中…'
            : isBackfillMode
              ? '保存这一天'
              : isEditPastMode
                ? '保存修改'
                : '保存今日记录'
        }}
      </button>
    </template>
  </main>
</template>
