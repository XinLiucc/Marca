<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

type Mode = 'login' | 'register'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

function safeRedirect(): string {
  const raw = route.query.redirect
  if (typeof raw !== 'string') return '/'
  // 仅接受站内路径，防止 open redirect
  return raw.startsWith('/') && !raw.startsWith('//') ? raw : '/'
}

const mode = ref<Mode>('login')
const email = ref('')
const password = ref('')
const nickname = ref('')
const submitting = ref(false)
const errorMsg = ref<string | null>(null)

function switchMode(next: Mode) {
  mode.value = next
  errorMsg.value = null
}

async function onSubmit() {
  errorMsg.value = null
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ email: email.value, password: password.value })
    } else {
      await auth.register({
        email: email.value,
        password: password.value,
        nickname: nickname.value || undefined,
      })
    }
    router.push(safeRedirect())
  } catch (err) {
    if (axios.isAxiosError(err)) {
      errorMsg.value = err.response?.data?.message ?? '网络错误，请稍后再试'
    } else {
      errorMsg.value = '未知错误'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center px-4">
    <div class="w-full max-w-sm rounded-3xl bg-white p-8 shadow-md">
      <header class="mb-6 text-center">
        <h1 class="text-2xl font-bold text-mint-600">Marca · 默刻</h1>
        <p class="mt-1 text-sm text-gray-500">完整或残缺，都是我们真实活过的证明</p>
      </header>

      <div class="mb-6 flex rounded-2xl bg-mint-50 p-1 text-sm font-medium">
        <button
          type="button"
          class="flex-1 rounded-xl py-2 transition"
          :class="mode === 'login' ? 'bg-white text-mint-600 shadow-sm' : 'text-gray-500'"
          @click="switchMode('login')"
        >
          登录
        </button>
        <button
          type="button"
          class="flex-1 rounded-xl py-2 transition"
          :class="mode === 'register' ? 'bg-white text-mint-600 shadow-sm' : 'text-gray-500'"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <form class="space-y-4" @submit.prevent="onSubmit">
        <div>
          <label class="mb-1 block text-sm text-gray-600">邮箱</label>
          <input
            v-model="email"
            type="email"
            required
            autocomplete="email"
            class="w-full rounded-2xl border border-gray-200 px-4 py-2 outline-none focus:border-mint-400"
            placeholder="hello@marca.app"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm text-gray-600">密码</label>
          <input
            v-model="password"
            type="password"
            required
            minlength="6"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            class="w-full rounded-2xl border border-gray-200 px-4 py-2 outline-none focus:border-mint-400"
            placeholder="至少 6 位"
          />
        </div>

        <div v-if="mode === 'register'">
          <label class="mb-1 block text-sm text-gray-600">昵称 <span class="text-gray-400">（可选）</span></label>
          <input
            v-model="nickname"
            type="text"
            maxlength="50"
            class="w-full rounded-2xl border border-gray-200 px-4 py-2 outline-none focus:border-mint-400"
            placeholder="留空则用邮箱前缀"
          />
        </div>

        <p v-if="errorMsg" class="text-sm text-red-500">{{ errorMsg }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-2xl bg-mint-500 py-2.5 font-semibold text-white shadow-sm transition hover:bg-mint-600 disabled:opacity-60"
        >
          {{ submitting ? '处理中…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>
    </div>
  </main>
</template>
