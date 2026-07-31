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

const changingPassword = ref(false)
const oldPwd = ref('')
const newPwd = ref('')
const confirmPwd = ref('')
const pwdSaving = ref(false)
const pwdError = ref('')
const pwdSuccessMsg = ref('')
const showOldPwd = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)

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

function startChangePassword() {
  oldPwd.value = ''
  newPwd.value = ''
  confirmPwd.value = ''
  pwdError.value = ''
  pwdSuccessMsg.value = ''
  showOldPwd.value = false
  showNewPwd.value = false
  showConfirmPwd.value = false
  changingPassword.value = true
}

function cancelChangePassword() {
  changingPassword.value = false
  pwdError.value = ''
}

async function submitChangePassword() {
  if (!oldPwd.value) {
    pwdError.value = '请输入旧密码'
    return
  }
  if (newPwd.value.length < 6 || newPwd.value.length > 64) {
    pwdError.value = '新密码长度需在 6~64 之间'
    return
  }
  if (newPwd.value !== confirmPwd.value) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  pwdSaving.value = true
  pwdError.value = ''
  try {
    await authApi.changePassword(oldPwd.value, newPwd.value)
    changingPassword.value = false
    pwdSuccessMsg.value = '密码修改成功'
  } catch (err) {
    const code = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
    pwdError.value = code === 'OLD_PASSWORD_INCORRECT' ? '旧密码错误' : '修改失败，请稍后重试'
  } finally {
    pwdSaving.value = false
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

      <!-- 修改密码 -->
      <section class="mb-6 rounded-3xl bg-white p-6">
        <h2 class="mb-4 text-sm font-medium text-gray-400">账号安全</h2>

        <div v-if="!changingPassword">
          <button
            class="rounded-full px-3 py-1 text-xs text-mint-600 hover:bg-mint-50"
            @click="startChangePassword"
          >
            修改密码
          </button>
          <p v-if="pwdSuccessMsg" class="mt-2 text-xs text-mint-600">{{ pwdSuccessMsg }}</p>
        </div>

        <div v-else class="space-y-3">
          <div class="relative">
            <input
              v-model="oldPwd"
              :type="showOldPwd ? 'text' : 'password'"
              placeholder="旧密码"
              class="w-full rounded-xl border border-gray-200 px-3 py-2 pr-10 text-sm focus:border-mint-400 focus:outline-none"
              :disabled="pwdSaving"
            />
            <button
              type="button"
              tabindex="-1"
              class="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-gray-400 hover:text-gray-600"
              @click="showOldPwd = !showOldPwd"
            >
              {{ showOldPwd ? '🙈' : '👁️' }}
            </button>
          </div>
          <div class="relative">
            <input
              v-model="newPwd"
              :type="showNewPwd ? 'text' : 'password'"
              placeholder="新密码（6~64 位）"
              class="w-full rounded-xl border border-gray-200 px-3 py-2 pr-10 text-sm focus:border-mint-400 focus:outline-none"
              :disabled="pwdSaving"
            />
            <button
              type="button"
              tabindex="-1"
              class="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-gray-400 hover:text-gray-600"
              @click="showNewPwd = !showNewPwd"
            >
              {{ showNewPwd ? '🙈' : '👁️' }}
            </button>
          </div>
          <div class="relative">
            <input
              v-model="confirmPwd"
              :type="showConfirmPwd ? 'text' : 'password'"
              placeholder="确认新密码"
              class="w-full rounded-xl border border-gray-200 px-3 py-2 pr-10 text-sm focus:border-mint-400 focus:outline-none"
              :disabled="pwdSaving"
              @keyup.enter="submitChangePassword"
            />
            <button
              type="button"
              tabindex="-1"
              class="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-gray-400 hover:text-gray-600"
              @click="showConfirmPwd = !showConfirmPwd"
            >
              {{ showConfirmPwd ? '🙈' : '👁️' }}
            </button>
          </div>
          <p v-if="pwdError" class="text-xs text-red-500">{{ pwdError }}</p>
          <div class="flex justify-end gap-2">
            <button
              class="rounded-full px-3 py-1 text-xs text-gray-400 hover:bg-gray-50"
              :disabled="pwdSaving"
              @click="cancelChangePassword"
            >
              取消
            </button>
            <button
              class="rounded-full px-3 py-1 text-xs text-mint-600 hover:bg-mint-50 disabled:opacity-50"
              :disabled="pwdSaving"
              @click="submitChangePassword"
            >
              {{ pwdSaving ? '保存中…' : '保存' }}
            </button>
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
