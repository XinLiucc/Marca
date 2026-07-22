<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { recordsApi, type RecordDto } from '@/api/records'
import { weatherOf, moodsOf } from '@/lib/weatherMood'
import { resolveMediaUrl } from '@/lib/mediaUrl'

// HomeView：纯仪表盘，只回答"今天写了没有"，不含任何写作逻辑。
// 今天写了 → 摘要式落地态（引言 + 天气心情 + 语音/图片徽标），点「查看完整
// 记录」才 push 去 RecordDetailView（不是 replace——这样详情页的返回按钮
// 才有地方可退，不会直接退出 App）。今天没写 → 一张「开始写」的提示卡。
// 写作流程本身在 WriteView (/write)，避免这个文件继续膨胀。
const auth = useAuthStore()
const router = useRouter()

const loading = ref(true)
const errorMsg = ref<string | null>(null)
const todayRecord = ref<RecordDto | null>(null)
const randoming = ref(false)

const hasToday = computed(() => todayRecord.value !== null)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 5) return '夜深了'
  if (h < 11) return '早安'
  if (h < 14) return '午安'
  if (h < 19) return '傍晚好'
  return '晚安'
})

const todayLabel = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return `${y}-${m}-${day}`
})

const weatherTag = computed(() => weatherOf(todayRecord.value?.weather))
const moodTags = computed(() => moodsOf(todayRecord.value?.moods))
const hasVoice = computed(() => !!todayRecord.value?.voiceUrl)
const imageCount = computed(() => todayRecord.value?.images.length ?? 0)

const categoryLabel: Record<string, string> = {
  event: '事件',
  emotion: '情绪',
  future: '未来',
}

// 摘要引言：优先自由记录，没有则取第一条有内容的问答，只挑一段做"翻开这一页"式的呈现
const firstAnsweredId = computed(() => {
  const rec = todayRecord.value
  if (!rec || rec.freeText?.trim()) return null
  return rec.answers.find((a) => a.answer && a.answer.trim().length > 0)?.id ?? null
})
const teaserText = computed(() => {
  const rec = todayRecord.value
  if (!rec) return ''
  if (rec.freeText && rec.freeText.trim()) return rec.freeText.trim()
  const firstAnswer = rec.answers.find((a) => a.answer && a.answer.trim().length > 0)
  return firstAnswer?.answer.trim() ?? ''
})

// 除了顶部引言之外，其余写了内容的问答也列出来（引言已经用过的那条不重复列）
const restAnswered = computed(() => {
  const rec = todayRecord.value
  if (!rec) return []
  return rec.answers.filter((a) => a.answer?.trim() && a.id !== firstAnsweredId.value)
})

const previewImages = computed(() => todayRecord.value?.images.slice(0, 3) ?? [])
const extraImageCount = computed(() => Math.max(0, imageCount.value - previewImages.value.length))

onMounted(load)

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    todayRecord.value = await recordsApi.today()
  } catch (e) {
    errorMsg.value = axios.isAxiosError(e) ? (e.response?.data?.message ?? '加载失败，稍后再试') : '加载失败，稍后再试'
  } finally {
    loading.value = false
  }
}

function goWrite() {
  router.push('/write')
}

function goViewToday() {
  if (!todayRecord.value) return
  router.push({ name: 'record-detail', params: { date: todayRecord.value.recordDate } })
}

async function pickRandom() {
  if (randoming.value) return
  randoming.value = true
  errorMsg.value = null
  try {
    const rec = await recordsApi.random(todayRecord.value?.recordDate)
    if (rec) {
      router.push({ name: 'record-detail', params: { date: rec.recordDate } })
    } else {
      errorMsg.value = '没有其他历史记录可以随机了'
    }
  } finally {
    randoming.value = false
  }
}
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <!-- 顶部：问候 + 导航 -->
    <header class="mb-8 flex items-end justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ todayLabel }}</p>
        <h1 class="text-2xl font-bold text-mint-600">{{ greeting }}，{{ auth.nickname ?? '默刻用户' }}</h1>
      </div>
      <nav class="flex items-center gap-2 text-xs text-gray-500">
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
        <RouterLink to="/random" class="rounded-full px-3 py-1 hover:bg-mint-50">随机</RouterLink>
        <RouterLink to="/settings" class="rounded-full px-3 py-1 hover:bg-mint-50">设置</RouterLink>
      </nav>
    </header>

    <!-- loading -->
    <div v-if="loading" class="rounded-3xl bg-white p-6 text-center text-sm text-gray-400">
      加载中…
    </div>

    <!-- error -->
    <div v-else-if="errorMsg && !hasToday" class="rounded-3xl bg-red-50 p-6 text-center text-sm text-red-500">
      {{ errorMsg }}
    </div>

    <template v-else>
      <!-- 今天已写：摘要落地态 -->
      <article v-if="hasToday" class="rounded-3xl bg-white p-6 shadow-sm">
        <div
          v-if="weatherTag || moodTags.length || hasVoice || imageCount > 0"
          class="mb-4 flex flex-wrap items-center gap-2 text-sm text-gray-600"
        >
          <span v-if="weatherTag" class="rounded-full bg-mint-50 px-3 py-1">
            {{ weatherTag.emoji }} {{ weatherTag.label }}
          </span>
          <span v-for="m in moodTags" :key="m.key" class="rounded-full bg-mint-50 px-3 py-1">
            {{ m.emoji }} {{ m.label }}
          </span>
          <span v-if="hasVoice" class="rounded-full bg-mint-50 px-3 py-1">🎤 语音</span>
          <span v-if="imageCount > 0" class="rounded-full bg-mint-50 px-3 py-1">📷 {{ imageCount }} 张</span>
        </div>
        <p v-if="teaserText" class="line-clamp-2 text-xl font-medium italic leading-relaxed text-gray-700">
          "{{ teaserText }}"
        </p>
        <p v-else class="text-sm text-gray-400">今天已经记下了，翻开看看吧</p>

        <!-- 其余写了内容的问答，摘一句展示 -->
        <ul v-if="restAnswered.length" class="mt-4 space-y-2">
          <li
            v-for="a in restAnswered"
            :key="a.id ?? a.question"
            class="rounded-2xl bg-mint-50/60 px-4 py-2.5 text-sm text-gray-600"
          >
            <span v-if="a.category" class="mr-2 rounded-full bg-mint-100 px-2 py-0.5 text-xs text-mint-600">
              {{ categoryLabel[a.category] ?? a.category }}
            </span>
            <span class="line-clamp-2 leading-relaxed">{{ a.answer }}</span>
          </li>
        </ul>

        <!-- 图片小样 -->
        <div v-if="previewImages.length" class="mt-4 flex gap-2">
          <div
            v-for="img in previewImages"
            :key="img.id ?? img.url"
            class="h-16 w-16 overflow-hidden rounded-xl bg-mint-50"
          >
            <img :src="resolveMediaUrl(img.url) ?? undefined" class="h-full w-full object-cover" />
          </div>
          <div
            v-if="extraImageCount > 0"
            class="flex h-16 w-16 items-center justify-center rounded-xl bg-mint-50 text-xs text-mint-500"
          >
            +{{ extraImageCount }}
          </div>
        </div>

        <button
          type="button"
          class="mt-5 text-xs text-mint-600 underline-offset-2 hover:underline"
          @click="goViewToday"
        >
          查看完整记录 →
        </button>
      </article>

      <!-- 今天还没写 -->
      <div v-else class="rounded-3xl bg-white p-10 text-center shadow-sm">
        <p class="mb-6 text-sm leading-relaxed text-gray-500">
          今天还没落笔，像一页干净的纸，安静地等着。
        </p>
        <button
          type="button"
          class="rounded-2xl bg-mint-500 px-6 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
          @click="goWrite"
        >
          开始
        </button>
      </div>

      <div class="mt-5 flex flex-wrap items-center justify-center gap-3">
        <button
          v-if="hasToday"
          type="button"
          class="rounded-2xl bg-mint-500 px-6 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
          @click="goWrite"
        >
          重新写
        </button>
        <button
          type="button"
          :disabled="randoming"
          class="rounded-2xl bg-white px-6 py-2.5 text-sm text-mint-600 shadow-sm transition hover:bg-mint-50 disabled:opacity-60"
          @click="pickRandom"
        >
          {{ randoming ? '寻一段往日…' : '重逢往日' }}
        </button>
      </div>

      <p v-if="errorMsg && hasToday" class="mt-3 text-center text-xs text-red-500">{{ errorMsg }}</p>
    </template>
  </main>
</template>
