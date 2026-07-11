<script setup lang="ts">
import { computed } from 'vue'
import type { RecordDto } from '@/api/records'

const props = defineProps<{
  year: number
  month: number // 1-12
  records: RecordDto[]
}>()

const emit = defineEmits<{ prev: []; next: [] }>()

const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六']

const recordDates = computed(() => new Set(props.records.map((r) => r.recordDate)))

// 避免 toISOString() 的 UTC 时区坑，本地字段手工拼接
function localTodayString(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return `${y}-${m}-${day}`
}
const today = localTodayString()

const isCurrentMonth = computed(() => {
  const [ty, tm] = today.split('-').map(Number)
  return props.year === ty && props.month === tm
})

interface Cell {
  date: string | null
  day: number | null
  hasRecord: boolean
  isToday: boolean
}

const cells = computed<Cell[]>(() => {
  const firstWeekday = new Date(props.year, props.month - 1, 1).getDay()
  const daysInMonth = new Date(props.year, props.month, 0).getDate()
  const list: Cell[] = []
  for (let i = 0; i < firstWeekday; i++) {
    list.push({ date: null, day: null, hasRecord: false, isToday: false })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const mm = props.month.toString().padStart(2, '0')
    const dd = d.toString().padStart(2, '0')
    const date = `${props.year}-${mm}-${dd}`
    list.push({ date, day: d, hasRecord: recordDates.value.has(date), isToday: date === today })
  }
  while (list.length % 7 !== 0) {
    list.push({ date: null, day: null, hasRecord: false, isToday: false })
  }
  return list
})
</script>

<template>
  <div class="rounded-3xl bg-white p-5 shadow-sm">
    <div class="mb-4 flex items-center justify-between">
      <button
        class="rounded-full px-2 py-1 text-gray-400 transition hover:bg-mint-50 hover:text-mint-600"
        @click="emit('prev')"
      >
        ‹
      </button>
      <p class="text-sm font-medium text-mint-600">{{ year }} 年 {{ month }} 月</p>
      <button
        class="rounded-full px-2 py-1 text-gray-400 transition hover:bg-mint-50 hover:text-mint-600 disabled:pointer-events-none disabled:opacity-0"
        :disabled="isCurrentMonth"
        @click="emit('next')"
      >
        ›
      </button>
    </div>

    <div class="grid grid-cols-7 gap-1.5 text-center">
      <span v-for="w in weekdayLabels" :key="w" class="text-[11px] text-gray-400">{{ w }}</span>

      <template v-for="(cell, i) in cells" :key="i">
        <div v-if="!cell.date" />
        <RouterLink
          v-else-if="cell.hasRecord"
          :to="{ name: 'record-detail', params: { date: cell.date } }"
          class="flex aspect-square items-center justify-center rounded-lg bg-mint-400 text-xs font-medium text-white transition hover:bg-mint-500"
          :class="{ 'ring-2 ring-mint-600 ring-offset-1': cell.isToday }"
        >
          {{ cell.day }}
        </RouterLink>
        <div
          v-else
          class="flex aspect-square items-center justify-center rounded-lg text-xs text-gray-300"
          :class="{ 'ring-2 ring-mint-200 ring-offset-1': cell.isToday }"
        >
          {{ cell.day }}
        </div>
      </template>
    </div>
  </div>
</template>
