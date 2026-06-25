<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { recordsApi, type RecordDto } from '@/api/records'
import RecordCard from '@/components/RecordCard.vue'

const record = ref<RecordDto | null>(null)
const loading = ref(false)
const noHistory = ref(false)

async function pickRandom() {
  loading.value = true
  noHistory.value = false
  try {
    const res = await recordsApi.random()
    if (!res) {
      noHistory.value = true
      record.value = null
    } else {
      record.value = res
    }
  } finally {
    loading.value = false
  }
}

const daysAgo = computed(() => {
  if (!record.value) return null
  const today = new Date()
  const d = new Date(record.value.recordDate)
  const diff = Math.round((today.getTime() - d.getTime()) / 86400000)
  if (diff <= 0) return '今天'
  if (diff < 30) return `${diff} 天前`
  if (diff < 365) return `${Math.round(diff / 30)} 个月前`
  return `${Math.round(diff / 365)} 年前`
})

onMounted(pickRandom)
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <header class="mb-6 flex items-baseline justify-between">
      <div>
        <h1 class="text-xl font-bold text-mint-600">随机回看</h1>
        <p v-if="daysAgo" class="text-xs text-gray-500">这是{{ daysAgo }}的你</p>
      </div>
      <nav class="flex gap-2 text-xs text-gray-500">
        <RouterLink to="/" class="rounded-full px-3 py-1 hover:bg-mint-50">今日</RouterLink>
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
      </nav>
    </header>

    <div v-if="loading" class="rounded-3xl bg-white p-8 text-center text-sm text-gray-400">
      抽一条…
    </div>
    <div v-else-if="noHistory" class="rounded-3xl bg-white p-8 text-center text-sm text-gray-400">
      还没有历史记录可以随机
    </div>
    <RecordCard v-else-if="record" :record="record" mode="full" />

    <button
      v-if="!noHistory"
      :disabled="loading"
      class="mt-6 w-full rounded-2xl bg-mint-500 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-mint-600 disabled:opacity-60"
      @click="pickRandom"
    >
      再随机一条
    </button>
  </main>
</template>
