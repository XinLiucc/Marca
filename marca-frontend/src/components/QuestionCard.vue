<script setup lang="ts">
import type { Question } from '@/api/questions'

defineProps<{
  question: Question
  index: number
}>()

const categoryLabel: Record<Question['category'], string> = {
  event: '事件',
  emotion: '情绪',
  future: '未来',
}
</script>

<template>
  <article class="rounded-3xl bg-white p-5 shadow-sm">
    <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
      <span class="rounded-full bg-mint-100 px-2 py-0.5">{{ categoryLabel[question.category] }}</span>
      <span class="text-gray-400">#{{ index + 1 }}</span>
    </header>
    <p class="text-base leading-relaxed text-gray-800">{{ question.content }}</p>
    <!-- 答题输入框由 feature/records 模块接入 -->
  </article>
</template>
