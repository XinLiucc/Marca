<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, type UserResponse } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const user = ref<UserResponse | null>(null)
const loading = ref(true)
const errorMsg = ref('')

const editing = ref(false)
const draft = ref('')
const saving = ref(false)
const saveError = ref('')

function formatCreatedAt(dt: string): string {
  // 后端返回 "2026-06-25T15:52:41"，直接拆字符串，避免时区换算
  const [y, m, d] = dt.slice(0, 10).split('-')
  return `${y} 年 ${Number(m)} 月 ${Number(d)} 日`
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    user.value = await authApi.me()
  } catch {
    errorMsg.value = '加载账号信息失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function startEdit() {
  draft.value = user.value?.nickname ?? ''
  saveError.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  saveError.value = ''
}

async function saveNickname() {
  const nickname = draft.value.trim()
  if (!nickname) {
    saveError.value = '昵称不能为空'
    return
  }
  if (nickname.length > 50) {
    saveError.value = '昵称不能超过 50 字'
    return
  }
  saving.value = true
  saveError.value = ''
  try {
    user.value = await authApi.updateProfile(nickname)
    auth.setNickname(user.value.nickname)
    editing.value = false
  } catch {
    saveError.value = '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(load)
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <header class="mb-6 flex items-center justify-between">
      <button
        class="rounded-full px-3 py-1 text-xs text-gray-500 transition hover:bg-mint-50 hover:text-mint-600"
        @click="router.back()"
      >
        ← 返回
      </button>
      <h1 class="text-xl font-bold text-mint-600">设置</h1>
      <nav class="flex gap-2 text-xs text-gray-500">
        <RouterLink to="/" class="rounded-full px-3 py-1 hover:bg-mint-50">今日</RouterLink>
        <RouterLink to="/timeline" class="rounded-full px-3 py-1 hover:bg-mint-50">时间轴</RouterLink>
      </nav>
    </header>

    <div v-if="loading" class="rounded-3xl bg-white p-6 text-center text-sm text-gray-400">
      加载中…
    </div>

    <div v-else-if="errorMsg" class="rounded-3xl bg-red-50 p-6 text-center text-sm text-red-500">
      {{ errorMsg }}
    </div>

    <template v-else-if="user">
      <!-- 账号信息 -->
      <section class="mb-6 rounded-3xl bg-white p-6">
        <h2 class="mb-4 text-sm font-medium text-gray-400">账号信息</h2>

        <div class="space-y-4 text-sm">
          <div class="flex items-center justify-between">
            <span class="text-gray-500">昵称</span>
            <div v-if="!editing" class="flex items-center gap-2">
              <span class="text-gray-800">{{ user.nickname }}</span>
              <button
                class="rounded-full px-3 py-1 text-xs text-mint-600 hover:bg-mint-50"
                @click="startEdit"
              >
                编辑
              </button>
            </div>
            <div v-else class="flex items-center gap-2">
              <input
                v-model="draft"
                maxlength="50"
                class="w-40 rounded-xl border border-gray-200 px-3 py-1 text-sm focus:border-mint-400 focus:outline-none"
                :disabled="saving"
                @keyup.enter="saveNickname"
              />
              <button
                class="rounded-full px-3 py-1 text-xs text-mint-600 hover:bg-mint-50 disabled:opacity-50"
                :disabled="saving"
                @click="saveNickname"
              >
                {{ saving ? '保存中…' : '保存' }}
              </button>
              <button
                class="rounded-full px-3 py-1 text-xs text-gray-400 hover:bg-gray-50"
                :disabled="saving"
                @click="cancelEdit"
              >
                取消
              </button>
            </div>
          </div>
          <p v-if="saveError" class="text-right text-xs text-red-500">{{ saveError }}</p>

          <div class="flex items-center justify-between">
            <span class="text-gray-500">邮箱</span>
            <span class="text-gray-800">{{ user.email }}</span>
          </div>

          <div class="flex items-center justify-between">
            <span class="text-gray-500">开始记录于</span>
            <span class="text-gray-800">{{ formatCreatedAt(user.createdAt) }}</span>
          </div>
        </div>
      </section>

      <!-- 退出登录 -->
      <section class="rounded-3xl bg-white p-6">
        <button
          class="w-full rounded-xl py-2 text-sm text-red-500 hover:bg-red-50"
          @click="logout"
        >
          退出登录
        </button>
      </section>
    </template>
  </main>
</template>
