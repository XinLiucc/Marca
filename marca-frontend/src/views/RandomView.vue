<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { recordsApi } from '@/api/records'

// /random 是个「中转页」：进来立即抽一条 → replace 到详情页 /record/:date
// 抽不到（无历史）→ 显示提示 + 回时间轴
const router = useRouter()
const noHistory = ref(false)
const errorMsg = ref<string | null>(null)

onMounted(async () => {
  try {
    const rec = await recordsApi.random()
    if (!rec) {
      noHistory.value = true
    } else {
      router.replace({ name: 'record-detail', params: { date: rec.recordDate } })
    }
  } catch {
    errorMsg.value = '加载失败，回时间轴再试'
  }
})
</script>

<template>
  <main class="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center px-4 text-center">
    <template v-if="noHistory">
      <p class="mb-4 text-sm text-gray-500">还没有历史记录可以随机</p>
      <RouterLink
        to="/"
        class="rounded-2xl bg-mint-500 px-5 py-2 text-sm font-medium text-white shadow-sm hover:bg-mint-600"
      >
        回今日写一条
      </RouterLink>
    </template>
    <template v-else-if="errorMsg">
      <p class="mb-4 text-sm text-red-500">{{ errorMsg }}</p>
      <RouterLink to="/timeline" class="text-xs text-mint-600 underline">回时间轴</RouterLink>
    </template>
    <p v-else class="text-sm text-gray-400">抽一条…</p>
  </main>
</template>
