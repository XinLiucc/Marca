<script setup lang="ts">
import { computed } from 'vue'
import type { RecordDto } from '@/api/records'
import { moodsOf, weatherOf } from '@/lib/weatherMood'
import { writtenAtLabel as computeWrittenAtLabel } from '@/lib/writtenAt'
import { resolveMediaUrl } from '@/lib/mediaUrl'

const props = defineProps<{
  record: RecordDto
  mode?: 'compact' | 'full'
}>()

const mode = computed(() => props.mode ?? 'compact')

const categoryLabel: Record<string, string> = {
  event: '事件',
  emotion: '情绪',
  future: '未来',
}

// compact 模式下的摘要文本：优先问答首条，否则自由记录，否则提示只有语音/图
const compactPreview = computed(() => {
  if (props.record.answers[0]) return props.record.answers[0].answer
  if (props.record.freeText) return props.record.freeText
  if (props.record.voiceUrl) return '（只有语音）'
  if (props.record.images.length) return '（只有图片）'
  return '（空记录）'
})

const isEmpty = computed(
  () =>
    !props.record.answers.length &&
    !props.record.voiceUrl &&
    !props.record.images.length &&
    !props.record.freeText,
)

const weatherEmoji = computed(() => weatherOf(props.record.weather)?.emoji ?? null)
const moodEmojis = computed(() => moodsOf(props.record.moods).map((m) => m.emoji))

const writtenAtLabel = computed(() => computeWrittenAtLabel(props.record))
</script>

<template>
  <article class="rounded-3xl bg-white p-5 shadow-sm">
    <header class="mb-3 flex items-baseline justify-between gap-3">
      <div class="flex items-baseline gap-2">
        <p class="text-base font-medium text-mint-600">{{ record.recordDate }}</p>
        <span v-if="weatherEmoji || moodEmojis.length" class="text-sm leading-none">
          <span v-if="weatherEmoji">{{ weatherEmoji }}</span>
          <span v-if="moodEmojis.length" class="ml-0.5">{{ moodEmojis.join('') }}</span>
        </span>
      </div>
      <span class="text-xs text-gray-400 text-right">
        <span v-if="record.answers.length">{{ record.answers.length }} 段</span>
        <span v-if="record.freeText">{{ record.answers.length ? ' · ' : '' }}还想说</span>
        <span v-if="record.voiceUrl"> · 语音</span>
        <span v-if="record.images.length"> · {{ record.images.length }} 图</span>
      </span>
    </header>
    <p v-if="writtenAtLabel" class="-mt-2 mb-3 text-[11px] text-gray-400">{{ writtenAtLabel }}</p>

    <!-- compact：摘要 -->
    <template v-if="mode === 'compact'">
      <p class="line-clamp-2 text-sm leading-relaxed text-gray-600">
        {{ compactPreview }}
      </p>
    </template>

    <!-- full：所有内容展开 -->
    <template v-else>
      <ul v-if="record.answers.length" class="space-y-3">
        <li v-for="(a, i) in record.answers" :key="a.id ?? i" class="rounded-2xl bg-mint-50/50 p-3">
          <div class="mb-1 flex items-center gap-2 text-xs text-mint-600">
            <span class="rounded-full bg-mint-100 px-2 py-0.5">{{ a.category ? categoryLabel[a.category] : '—' }}</span>
            <span class="text-gray-400">{{ a.question }}</span>
          </div>
          <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{{ a.answer }}</p>
        </li>
      </ul>

      <div v-if="record.freeText" class="mt-3 rounded-2xl bg-mint-50/50 p-3">
        <p class="mb-2 text-xs text-mint-600">还想说</p>
        <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{{ record.freeText }}</p>
      </div>

      <div v-if="record.voiceUrl" class="mt-3 rounded-2xl bg-mint-50/50 p-3">
        <p class="mb-2 text-xs text-mint-600">语音</p>
        <audio :src="resolveMediaUrl(record.voiceUrl) ?? undefined" controls class="w-full" />
      </div>

      <div v-if="record.images.length" class="mt-3 rounded-2xl bg-mint-50/50 p-3">
        <p class="mb-2 text-xs text-mint-600">图片</p>
        <div class="grid grid-cols-3 gap-2">
          <a
            v-for="img in record.images"
            :key="img.id ?? img.url"
            :href="resolveMediaUrl(img.url) ?? undefined"
            target="_blank"
            rel="noopener"
            class="block aspect-square overflow-hidden rounded-2xl bg-white"
          >
            <img :src="resolveMediaUrl(img.url) ?? undefined" class="h-full w-full object-cover" />
          </a>
        </div>
      </div>

      <p v-if="isEmpty" class="text-sm text-gray-400">（空记录）</p>
    </template>
  </article>
</template>
