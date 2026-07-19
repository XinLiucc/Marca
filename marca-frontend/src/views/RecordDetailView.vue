<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { recordsApi, type RecordDto } from '@/api/records'
import { moodsOf, weatherOf } from '@/lib/weatherMood'
import { writtenAtLabel as computeWrittenAtLabel } from '@/lib/writtenAt'
import { canBackfill as computeCanBackfill } from '@/lib/backfillWindow'
import { resolveMediaUrl } from '@/lib/mediaUrl'

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

const weatherTag = computed(() => weatherOf(record.value?.weather))
const moodTags = computed(() => moodsOf(record.value?.moods))

// 是否在看今天的记录（只有今天能从这里跳到"重新写"）
const isToday = computed(() => props.date === localTodayString())
function localTodayString(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return `${y}-${m}-${day}`
}

function goEdit() {
  router.push({ path: '/', query: { edit: '1' } })
}

function goEditPast() {
  router.push({ path: '/', query: { editDate: props.date } })
}

function goWriteToday() {
  router.push('/')
}

const canBackfill = computed(() => computeCanBackfill(props.date))

function goBackfill() {
  router.push({ path: '/', query: { backfill: props.date } })
}

const writtenAtLabel = computed(() => (record.value ? computeWrittenAtLabel(record.value) : null))

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
  errorMsg.value = null
  try {
    // 把当前正在看的这条也加进 exclude，后端再叠加 today，自然不抽到重复
    const rec = await recordsApi.random(props.date)
    if (rec) {
      router.replace({ name: 'record-detail', params: { date: rec.recordDate } })
    } else {
      errorMsg.value = '没有其他历史记录可以随机了'
    }
  } finally {
    randoming.value = false
  }
}

// 删除不受补写窗口限制（删除不伪造历史），但要二次确认——不可逆操作
const confirmingDelete = ref(false)
const deleting = ref(false)

function askDelete() {
  confirmingDelete.value = true
}

function cancelDelete() {
  confirmingDelete.value = false
}

async function confirmDelete() {
  deleting.value = true
  errorMsg.value = null
  try {
    await recordsApi.remove(props.date)
    router.replace('/timeline')
  } catch (e) {
    errorMsg.value = axios.isAxiosError(e) ? (e.response?.data?.message ?? '删除失败') : '删除失败'
    confirmingDelete.value = false
  } finally {
    deleting.value = false
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
      <p class="mb-6 text-sm text-gray-500">
        {{ isToday ? '今天还没写' : '这天没有日记' }}
      </p>
      <button
        v-if="isToday"
        class="rounded-2xl bg-mint-500 px-5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
        @click="goWriteToday"
      >
        去写今天的
      </button>
      <template v-else>
        <button
          v-if="canBackfill"
          class="rounded-2xl bg-mint-500 px-5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
          @click="goBackfill"
        >
          补写这一天
        </button>
        <div v-else>
          <button
            disabled
            class="cursor-not-allowed rounded-2xl bg-gray-100 px-5 py-2 text-sm font-medium text-gray-400"
          >
            补写这一天
          </button>
          <p class="mt-3 text-xs leading-relaxed text-gray-400">
            记忆也会过期，像枝头放久的果子——写下此刻，趁它还新鲜。
          </p>
        </div>
        <RouterLink
          to="/timeline"
          class="mt-4 inline-block rounded-2xl bg-white px-5 py-2 text-sm text-mint-600 shadow-sm transition hover:bg-mint-50"
        >
          回时间轴
        </RouterLink>
      </template>
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
        <div v-if="weatherTag || moodTags.length" class="mt-3 flex flex-wrap items-center gap-2 text-sm text-gray-600">
          <span v-if="weatherTag" class="rounded-full bg-mint-50 px-3 py-1">
            {{ weatherTag.emoji }} {{ weatherTag.label }}
          </span>
          <span
            v-for="m in moodTags"
            :key="m.key"
            class="rounded-full bg-mint-50 px-3 py-1"
          >
            {{ m.emoji }} {{ m.label }}
          </span>
        </div>
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
        <audio :src="resolveMediaUrl(record.voiceUrl) ?? undefined" controls class="w-full" />
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
            :href="resolveMediaUrl(img.url) ?? undefined"
            target="_blank"
            rel="noopener"
            class="block aspect-square overflow-hidden rounded-2xl bg-mint-50"
          >
            <img :src="resolveMediaUrl(img.url) ?? undefined" class="h-full w-full object-cover" />
          </a>
        </div>
      </section>

      <!-- 底部操作：今天 = 重新写；窗口内的过去某天 = 编辑 + 重逢往日；窗口外 = 只有重逢往日（彻底锁定）-->
      <div class="mt-8 flex flex-wrap items-center justify-center gap-3 text-center">
        <button
          v-if="isToday"
          class="rounded-2xl bg-mint-500 px-6 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
          @click="goEdit"
        >
          重新写
        </button>
        <button
          v-if="!isToday && canBackfill"
          class="rounded-2xl bg-mint-500 px-6 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600"
          @click="goEditPast"
        >
          编辑
        </button>
        <button
          v-if="!isToday"
          :disabled="randoming"
          class="rounded-2xl bg-white px-6 py-2.5 text-sm text-mint-600 shadow-sm transition hover:bg-mint-50 disabled:opacity-60"
          @click="pickRandom"
        >
          {{ randoming ? '寻一段往日…' : '重逢往日' }}
        </button>
      </div>

      <!-- 删除：二次确认，不可逆，不受补写窗口限制 -->
      <div class="mt-4 text-center">
        <button
          v-if="!confirmingDelete"
          class="text-xs text-gray-400 transition hover:text-red-500"
          @click="askDelete"
        >
          删除这一天
        </button>
        <div v-else class="flex flex-col items-center gap-2">
          <p class="text-xs text-gray-500">真的要删除这一天吗？删掉就找不回来了。</p>
          <div class="flex items-center gap-3">
            <button
              class="rounded-2xl px-4 py-1.5 text-xs text-gray-500 transition hover:bg-mint-50"
              @click="cancelDelete"
            >
              取消
            </button>
            <button
              :disabled="deleting"
              class="rounded-2xl bg-red-500 px-4 py-1.5 text-xs font-medium text-white shadow-sm transition hover:bg-red-600 disabled:opacity-60"
              @click="confirmDelete"
            >
              {{ deleting ? '删除中…' : '确定删除' }}
            </button>
          </div>
        </div>
      </div>

      <p v-if="errorMsg" class="mt-3 text-center text-xs text-red-500">{{ errorMsg }}</p>
    </template>
  </main>
</template>
