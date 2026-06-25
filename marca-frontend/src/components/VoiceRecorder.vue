<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import { recordsApi } from '@/api/records'
import { useRecorder } from '@/composables/useRecorder'

const props = defineProps<{
  initialUrl?: string | null
  initialDuration?: number | null
}>()

const emit = defineEmits<{
  uploaded: [{ voiceUrl: string; duration: number }]
  cleared: []
}>()

// 解构 useRecorder 的 refs 到顶层：模板里直接用 objectUrl/durationSec 等，
// 自动 unwrap 且 TS 推断正确
const {
  status: recStatus,
  errorMsg: recErrorMsg,
  durationSec,
  blob: recBlob,
  objectUrl,
  start: startRecord,
  stop: stopRecord,
  reset: resetRecord,
} = useRecorder()

const uploading = ref(false)
const uploadError = ref<string | null>(null)

// 已有语音（编辑模式回显）
const existingUrl = ref<string | null>(props.initialUrl ?? null)
const existingDuration = ref<number | null>(props.initialDuration ?? null)

watch(
  () => props.initialUrl,
  (v) => {
    existingUrl.value = v ?? null
    existingDuration.value = props.initialDuration ?? null
  },
)

// 「重录中」标记：暂时隐藏已有语音 UI，让录音 UI 显示；
// 用户取消则恢复到已有语音，上传则替换。
const rerecording = ref(false)

const hasExisting = computed(() => !!existingUrl.value && !rerecording.value)
const hasNew = computed(() => recStatus.value === 'stopped' && !!recBlob.value)
const isRecording = computed(() => recStatus.value === 'recording')

function fmt(sec: number): string {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

async function onMainAction() {
  if (isRecording.value) {
    await stopRecord()
    return
  }
  // 上一次试听里的录音清掉，重新录一段
  if (hasNew.value) {
    resetRecord()
  }
  await startRecord()
}

/** 从「已有语音」点重录 → 切到录音 UI 并立即开始录制 */
async function onRerecord() {
  rerecording.value = true
  resetRecord()
  await startRecord()
}

/** 录音过程中或预览中放弃，回到原来那段（如果有） */
function onCancelRerecord() {
  resetRecord()
  rerecording.value = false
}

async function onUpload() {
  if (!recBlob.value) return
  uploading.value = true
  uploadError.value = null
  try {
    const res = await recordsApi.uploadVoice(recBlob.value, durationSec.value)
    existingUrl.value = res.voiceUrl
    existingDuration.value = res.duration ?? durationSec.value
    emit('uploaded', { voiceUrl: res.voiceUrl, duration: existingDuration.value ?? 0 })
    resetRecord()
    rerecording.value = false
  } catch (e) {
    uploadError.value = axios.isAxiosError(e)
      ? (e.response?.data?.message ?? '上传失败')
      : '上传失败'
  } finally {
    uploading.value = false
  }
}

function onClearExisting() {
  existingUrl.value = null
  existingDuration.value = null
  emit('cleared')
}
</script>

<template>
  <section class="rounded-3xl bg-white p-5 shadow-sm">
    <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
      <span class="rounded-full bg-mint-100 px-2 py-0.5">语音</span>
      <span class="text-gray-400">想说就说，留空也可以</span>
    </header>

    <!-- 已有语音（来自历史 / 刚上传）-->
    <div v-if="hasExisting" class="space-y-3">
      <div class="flex items-center justify-between rounded-2xl bg-mint-50/60 p-3">
        <audio :src="existingUrl ?? undefined" controls class="h-9 w-full max-w-xs" />
        <span class="ml-3 text-xs text-gray-500">{{ existingDuration ? fmt(existingDuration) : '' }}</span>
      </div>
      <div class="flex gap-2">
        <button
          type="button"
          class="rounded-2xl bg-white px-4 py-1.5 text-xs text-mint-600 shadow-sm hover:bg-mint-50"
          @click="onClearExisting"
        >
          删除
        </button>
        <button
          type="button"
          class="rounded-2xl bg-white px-4 py-1.5 text-xs text-mint-600 shadow-sm hover:bg-mint-50"
          @click="onRerecord"
        >
          重录
        </button>
      </div>
    </div>

    <!-- 录音中 / 待录 / 待上传 -->
    <div v-else class="space-y-3">
      <!-- 新录音预览（未上传） -->
      <div v-if="hasNew" class="rounded-2xl bg-mint-50/60 p-3">
        <audio :src="objectUrl ?? undefined" controls class="h-9 w-full" />
        <p class="mt-1 text-xs text-gray-500">{{ fmt(durationSec) }}</p>
      </div>

      <div class="flex items-center gap-3">
        <button
          type="button"
          :disabled="uploading"
          class="flex h-14 w-14 items-center justify-center rounded-full text-2xl shadow-sm transition disabled:opacity-60"
          :class="isRecording ? 'animate-pulse bg-red-500 text-white' : 'bg-mint-500 text-white hover:bg-mint-600'"
          @click="onMainAction"
        >
          {{ isRecording ? '■' : '●' }}
        </button>
        <div class="flex-1 text-sm">
          <p v-if="isRecording" class="text-red-500">录音中… {{ fmt(durationSec) }}</p>
          <p v-else-if="hasNew" class="text-gray-600">已录 {{ fmt(durationSec) }}，可上传或重录</p>
          <p v-else class="text-gray-500">点圆点开始录音</p>
        </div>
        <button
          v-if="hasNew"
          type="button"
          :disabled="uploading"
          class="rounded-2xl bg-mint-500 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-mint-600 disabled:opacity-60"
          @click="onUpload"
        >
          {{ uploading ? '上传中…' : '上传' }}
        </button>
      </div>

      <p v-if="recErrorMsg" class="text-xs text-red-500">{{ recErrorMsg }}</p>
      <p v-if="uploadError" class="text-xs text-red-500">{{ uploadError }}</p>

      <button
        v-if="rerecording"
        type="button"
        class="text-xs text-gray-400 underline hover:text-mint-600"
        @click="onCancelRerecord"
      >
        取消，保留原来那段
      </button>
    </div>
  </section>
</template>
