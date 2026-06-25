<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <main class="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center px-4 text-center">
    <h1 class="text-3xl font-bold text-mint-600">你好，{{ auth.nickname ?? '默刻用户' }}</h1>
    <p class="mt-3 text-gray-500">问答、语音、时间轴页面将由后续模块实现</p>
    <button
      class="mt-8 rounded-2xl bg-white px-6 py-2 text-sm font-medium text-mint-600 shadow-sm transition hover:bg-mint-100"
      @click="onLogout"
    >
      退出登录
    </button>
  </main>
</template>
