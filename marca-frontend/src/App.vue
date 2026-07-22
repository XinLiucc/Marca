<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { Capacitor } from '@capacitor/core'
import { App as CapacitorApp } from '@capacitor/app'
import type { PluginListenerHandle } from '@capacitor/core'

const router = useRouter()
const showExitConfirm = ref(false)

let backButtonHandle: PluginListenerHandle | undefined

onMounted(async () => {
  if (!Capacitor.isNativePlatform()) return
  backButtonHandle = await CapacitorApp.addListener('backButton', () => {
    if (router.currentRoute.value.name === 'home') {
      showExitConfirm.value = true
    } else {
      router.back()
    }
  })
})

onUnmounted(() => {
  backButtonHandle?.remove()
})

function confirmExit() {
  CapacitorApp.exitApp()
}

function cancelExit() {
  showExitConfirm.value = false
}
</script>

<template>
  <RouterView />

  <div
    v-if="showExitConfirm"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6"
    @click.self="cancelExit"
  >
    <div class="w-full max-w-xs rounded-3xl bg-white p-6 text-center shadow-lg">
      <p class="mb-6 text-sm text-gray-700">确定要退出 Marca 吗？</p>
      <div class="flex gap-3">
        <button
          type="button"
          class="flex-1 rounded-2xl bg-gray-100 py-2.5 text-sm text-gray-600 transition hover:bg-gray-200"
          @click="cancelExit"
        >
          取消
        </button>
        <button
          type="button"
          class="flex-1 rounded-2xl bg-mint-500 py-2.5 text-sm font-medium text-white transition hover:bg-mint-600"
          @click="confirmExit"
        >
          退出
        </button>
      </div>
    </div>
  </div>
</template>
