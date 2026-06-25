<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { questionsApi, type Question } from '@/api/questions'
import QuestionCountPicker from '@/components/QuestionCountPicker.vue'
import QuestionCard from '@/components/QuestionCard.vue'

const router = useRouter()
const auth = useAuthStore()

const count = ref(3)
const questions = ref<Question[]>([])
const loading = ref(false)
const errorMsg = ref<string | null>(null)
const today = ref<string>('')

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const res = await questionsApi.today(count.value)
    questions.value = res.questions
    today.value = res.date
  } catch (e) {
    errorMsg.value = '问题加载失败，稍后重试'
  } finally {
    loading.value = false
  }
}

watch(count, load, { immediate: true })

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8">
    <header class="mb-6 flex items-end justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ today || '今天' }}</p>
        <h1 class="text-2xl font-bold text-mint-600">你好，{{ auth.nickname ?? '默刻用户' }}</h1>
      </div>
      <button
        class="rounded-full px-3 py-1 text-xs text-gray-400 transition hover:text-mint-600"
        @click="onLogout"
      >
        退出
      </button>
    </header>

    <section class="mb-4 flex items-center justify-between">
      <span class="text-sm text-gray-600">今天答几题</span>
      <QuestionCountPicker v-model="count" />
    </section>

    <div v-if="loading" class="rounded-3xl bg-white p-6 text-center text-sm text-gray-400">
      加载中…
    </div>
    <div v-else-if="errorMsg" class="rounded-3xl bg-red-50 p-6 text-center text-sm text-red-500">
      {{ errorMsg }}
    </div>
    <div v-else class="space-y-3">
      <QuestionCard
        v-for="(q, i) in questions"
        :key="q.id"
        :question="q"
        :index="i"
      />
    </div>

    <button
      class="mt-6 w-full rounded-2xl bg-mint-300 py-3 text-sm font-medium text-white opacity-70"
      disabled
      title="保存功能由 feature/records 模块接入"
    >
      保存今日记录（待 records 模块接入）
    </button>
  </main>
</template>
