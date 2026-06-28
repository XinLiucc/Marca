<script setup lang="ts">
import { WEATHERS, MOODS } from '@/lib/weatherMood'

const props = defineProps<{
  weather: string | null
  moods: string[]
}>()

const emit = defineEmits<{
  'update:weather': [value: string | null]
  'update:moods': [value: string[]]
}>()

function pickWeather(key: string) {
  // 单选：再点选中的就取消
  emit('update:weather', props.weather === key ? null : key)
}

function toggleMood(key: string) {
  // 多选：toggle
  const set = new Set(props.moods)
  if (set.has(key)) set.delete(key)
  else set.add(key)
  // 按预设顺序排序，避免每次点击次序乱
  const order = new Map(MOODS.map((m, i) => [m.key, i]))
  const next = [...set].sort((a, b) => (order.get(a) ?? 0) - (order.get(b) ?? 0))
  emit('update:moods', next)
}
</script>

<template>
  <section class="rounded-3xl bg-white p-4 shadow-sm">
    <!-- 天气 -->
    <div class="mb-3">
      <p class="mb-2 text-[11px] text-gray-400">今天 · 天气</p>
      <div class="flex flex-wrap gap-1.5">
        <button
          v-for="w in WEATHERS"
          :key="w.key"
          type="button"
          class="flex min-w-[44px] flex-col items-center justify-center gap-0.5 rounded-2xl px-2 py-1.5 transition"
          :class="
            weather === w.key
              ? 'bg-mint-500 text-white shadow-sm'
              : 'bg-mint-50 text-gray-600 hover:bg-mint-100'
          "
          @click="pickWeather(w.key)"
        >
          <span class="text-lg leading-none">{{ w.emoji }}</span>
          <span class="text-[10px] leading-none">{{ w.label }}</span>
        </button>
      </div>
    </div>

    <!-- 心情 -->
    <div>
      <p class="mb-2 text-[11px] text-gray-400">此刻 · 心情（可多选）</p>
      <div class="flex flex-wrap gap-1.5">
        <button
          v-for="m in MOODS"
          :key="m.key"
          type="button"
          class="flex min-w-[44px] flex-col items-center justify-center gap-0.5 rounded-2xl px-2 py-1.5 transition"
          :class="
            moods.includes(m.key)
              ? 'bg-mint-500 text-white shadow-sm'
              : 'bg-mint-50 text-gray-600 hover:bg-mint-100'
          "
          @click="toggleMood(m.key)"
        >
          <span class="text-lg leading-none">{{ m.emoji }}</span>
          <span class="text-[10px] leading-none">{{ m.label }}</span>
        </button>
      </div>
    </div>
  </section>
</template>
