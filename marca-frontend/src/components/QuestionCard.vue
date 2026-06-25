<script setup lang="ts">
import type { Question } from '@/api/questions'

const props = defineProps<{
  question: Question
  index: number
  modelValue: string
  readonly?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const categoryLabel: Record<Question['category'], string> = {
  event: '事件',
  emotion: '情绪',
  future: '未来',
}

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <article class="rounded-3xl bg-white p-5 shadow-sm">
    <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
      <span class="rounded-full bg-mint-100 px-2 py-0.5">{{ categoryLabel[question.category] }}</span>
      <span class="text-gray-400">#{{ index + 1 }}</span>
    </header>
    <p class="mb-3 text-base leading-relaxed text-gray-800">{{ question.content }}</p>

    <textarea
      v-if="!readonly"
      :value="modelValue"
      rows="3"
      placeholder="想到什么就写什么，留空就跳过这题"
      class="min-h-[88px] w-full resize-none rounded-2xl border border-mint-200 bg-mint-50 p-3 text-sm leading-relaxed text-gray-800 placeholder:text-mint-400 outline-none transition focus:border-mint-500 focus:bg-white focus:ring-2 focus:ring-mint-100"
      @input="onInput"
    />
    <p
      v-else
      class="whitespace-pre-wrap rounded-2xl bg-mint-50/40 p-3 text-sm leading-relaxed text-gray-700"
    >
      {{ modelValue || '（这题没答）' }}
    </p>
  </article>
</template>
