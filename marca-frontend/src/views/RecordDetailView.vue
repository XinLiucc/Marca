<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { recordsApi, type RecordDto } from '@/api/records'

const props = defineProps<{
  date: string
}>()

const router = useRouter()

const record = ref<RecordDto | null>(null)
const loading = ref(true)
const notFound = ref(false)
const errorMsg = ref<string | null>(null)
const randoming = ref(false)

const categoryLabel: Record<string, string> = {
  event: '事件',
  emotion: '情绪',
  future: '未来',
}

const writtenAtLabel = computed(() => {
  if (!record.value?.createdAt) return null
  const createdDate = record.value.createdAt.slice(0, 10)
  if (createdDate === record.value.recordDate) return null
  const d = new Date(record.value.createdAt)
  const hh = d.getHours().toString().padStart(2, '0')
  const mm = d.getMinutes().toString().padStart(2, '0')
  const period = d.getHours() < 5 ? '凌晨' : ''
  return `${period} ${hh}:${mm} 写的`.trim()
})

async function load(date: string) {
  loading.value = true
  notFound.value = false
  errorMsg.value = null
  record.value = null
  try {
    const rec = await recordsApi.byDate(date)
    if (!rec) {
      notFound.value = true
    } else {
      record.value = rec
    }
  } catch (e) {
    errorMsg.value = axios.isAxiosError(e) ? (e.response?.data?.message ?? '加载失败') : '加载失败'
  } finally {
    loading.value = false
  }
}

async function pickRandom() {
  if (randoming.value) return
  randoming.value = true
  try {
    const rec = await recordsApi.random()
    if (rec && rec.recordDate !== props.date) {
      router.replace({ name: 'record-detail', params: { date: rec.recordDate } })
    } else if (rec) {
      // 抽到同一条（小概率），强提示但不跳转
      errorMsg.value = '又抽到同一条了，再点一下试试'
    } else {
      errorMsg.value = '还没有其他历史记录可以随机'
    }
  } finally {
    randoming.value = false
  }
}

onMounted(() => load(props.date))
watch(() => props.date, (d) => load(d))
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <!-- 顶部：返回 + 导航 -->
    <header class="mb-6 flex items-center justify-between">
      <button
        class="rounded-full px-3 py-1 text-xs text-gray-500 transition hover:bg-mint-50 hover:text-mint-600"
        @click="router.back()"
      >
        ← 返回
      </button>
      <nav class="flex gap-2 text-xs text-gray-500">
        <RouterLink to="/" class="rounded-full px-3 py-1 hover:bg-mint-50">今日</RouterLink>
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
      </nav>
    </header>

    <!-- loading -->
    <div v-if="loading" class="rounded-3xl bg-white p-8 text-center text-sm text-gray-400">
      加载中…
    </div>

    <!-- 不存在 -->
    <div v-else-if="notFound" class="rounded-3xl bg-white p-10 text-center">
      <p class="mb-2 text-base text-mint-600">{{ date }}</p>
      <p class="mb-6 text-sm text-gray-500">这天没有日记</p>
      <RouterLink
        to="/timeline"
        class="inline-block rounded-2xl bg-mint-500 px-5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
      >
        回时间轴
      </RouterLink>
    </div>

    <!-- 错误 -->
    <div v-else-if="errorMsg && !record" class="rounded-3xl bg-red-50 p-6 text-center text-sm text-red-500">
      {{ errorMsg }}
    </div>

    <!-- 正常展示 -->
    <template v-else-if="record">
      <!-- 标题 -->
      <header class="mb-6">
        <h1 class="text-3xl font-bold text-mint-600">{{ record.recordDate }}</h1>
        <p v-if="writtenAtLabel" class="mt-1 text-xs text-gray-400">{{ writtenAtLabel }}</p>
      </header>

      <!-- 问答 -->
      <section v-if="record.answers.length" class="space-y-4">
        <article
          v-for="(a, i) in record.answers"
          :key="a.id ?? i"
          class="rounded-3xl bg-white p-6 shadow-sm"
        >
          <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">{{ a.category ? categoryLabel[a.category] : '—' }}</span>
            <span class="text-gray-400">{{ a.question }}</span>
          </header>
          <p class="whitespace-pre-wrap text-base leading-relaxed text-gray-800">{{ a.answer }}</p>
        </article>
      </section>

      <!-- 自由记录 -->
      <section v-if="record.freeText" class="mt-4 rounded-3xl bg-white p-6 shadow-sm">
        <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
          <span class="rounded-full bg-mint-100 px-2 py-0.5">还想说</span>
        </header>
        <p class="whitespace-pre-wrap text-base leading-relaxed text-gray-800">{{ record.freeText }}</p>
      </section>

      <!-- 语音 -->
      <section v-if="record.voiceUrl" class="mt-4 rounded-3xl bg-white p-6 shadow-sm">
        <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
          <span class="rounded-full bg-mint-100 px-2 py-0.5">语音</span>
        </header>
        <audio :src="record.voiceUrl" controls class="w-full" />
      </section>

      <!-- 图片 -->
      <section v-if="record.images.length" class="mt-4 rounded-3xl bg-white p-6 shadow-sm">
        <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
          <span class="rounded-full bg-mint-100 px-2 py-0.5">图片</span>
        </header>
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <a
            v-for="img in record.images"
            :key="img.id ?? img.url"
            :href="img.url"
            target="_blank"
            rel="noopener"
            class="block aspect-square overflow-hidden rounded-2xl bg-mint-50"
          >
            <img :src="img.url" class="h-full w-full object-cover" />
          </a>
        </div>
      </section>

      <!-- 底部 random -->
      <div class="mt-8 text-center">
        <button
          :disabled="randoming"
          class="rounded-2xl bg-white px-5 py-2 text-sm text-mint-600 shadow-sm transition hover:bg-mint-50 disabled:opacity-60"
          @click="pickRandom"
        >
          {{ randoming ? '抽一条…' : '🎲 再来一条' }}
        </button>
        <p v-if="errorMsg" class="mt-3 text-xs text-red-500">{{ errorMsg }}</p>
      </div>
    </template>
  </main>
</template>
