<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { recordsApi, type RecordDto } from '@/api/records'
import RecordCard from '@/components/RecordCard.vue'
import MonthHeatmap from '@/components/MonthHeatmap.vue'

const now = new Date()
const viewYear = ref(now.getFullYear())
const viewMonth = ref(now.getMonth() + 1) // 1-12

const items = ref<RecordDto[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = await recordsApi.month(viewYear.value, viewMonth.value)
  } finally {
    loading.value = false
  }
}

function prevMonth() {
  if (viewMonth.value === 1) {
    viewMonth.value = 12
    viewYear.value -= 1
  } else {
    viewMonth.value -= 1
  }
}

function nextMonth() {
  if (viewMonth.value === 12) {
    viewMonth.value = 1
    viewYear.value += 1
  } else {
    viewMonth.value += 1
  }
}

watch([viewYear, viewMonth], load)
onMounted(load)
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <header class="mb-6 flex items-baseline justify-between">
      <div>
        <h1 class="text-xl font-bold text-mint-600">时间轴</h1>
        <p class="text-xs text-gray-500">这个月 {{ items.length }} 条记录</p>
      </div>
      <nav class="flex gap-2 text-xs text-gray-500">
        <RouterLink to="/" class="rounded-full px-3 py-1 hover:bg-mint-50">今日</RouterLink>
        <RouterLink to="/random" class="rounded-full px-3 py-1 hover:bg-mint-50">随机</RouterLink>
      </nav>
    </header>

    <MonthHeatmap
      class="mb-6"
      :year="viewYear"
      :month="viewMonth"
      :records="items"
      @prev="prevMonth"
      @next="nextMonth"
    />

    <div v-if="!loading && !items.length" class="rounded-3xl bg-white p-8 text-center text-sm text-gray-400">
      这个月还没有记录
    </div>

    <div class="space-y-3">
      <RouterLink
        v-for="r in items"
        :key="r.id"
        :to="{ name: 'record-detail', params: { date: r.recordDate } }"
        class="block transition hover:-translate-y-0.5 hover:shadow-md"
      >
        <RecordCard :record="r" mode="compact" />
      </RouterLink>
    </div>
  </main>
</template>
