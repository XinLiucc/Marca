import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi, type LoginPayload, type RegisterPayload } from '@/api/auth'

const TOKEN_KEY = 'marca_token'
const NICKNAME_KEY = 'marca_nickname'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const nickname = ref<string | null>(localStorage.getItem(NICKNAME_KEY))

  const isAuthenticated = computed(() => !!token.value)

  function setSession(t: string, n: string) {
    token.value = t
    nickname.value = n
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(NICKNAME_KEY, n)
  }

  async function login(payload: LoginPayload) {
    const res = await authApi.login(payload)
    setSession(res.token, res.nickname)
  }

  async function register(payload: RegisterPayload) {
    await authApi.register(payload)
    // 注册成功后顺手登录，体验更顺
    await login({ email: payload.email, password: payload.password })
  }

  function setNickname(n: string) {
    nickname.value = n
    localStorage.setItem(NICKNAME_KEY, n)
  }

  function logout() {
    token.value = null
    nickname.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NICKNAME_KEY)
  }

  return { token, nickname, isAuthenticated, login, register, setNickname, logout }
})
