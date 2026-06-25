import { ref } from 'vue'

export type RecorderStatus = 'idle' | 'requesting' | 'recording' | 'stopped' | 'error'

/**
 * Web MediaRecorder 封装。后续接 Capacitor 麦克风插件时，
 * 在这里增加平台分支，组件接口保持不变。
 */
export function useRecorder() {
  const status = ref<RecorderStatus>('idle')
  const errorMsg = ref<string | null>(null)
  const durationSec = ref(0)
  const blob = ref<Blob | null>(null)
  const objectUrl = ref<string | null>(null)
  const mime = ref<string>('')

  let stream: MediaStream | null = null
  let recorder: MediaRecorder | null = null
  let chunks: BlobPart[] = []
  let startedAt = 0
  let timerId: number | null = null
  let stopResolver: (() => void) | null = null

  function pickMime(): string {
    const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg']
    for (const c of candidates) {
      if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(c)) return c
    }
    return ''
  }

  async function start() {
    if (status.value === 'recording' || status.value === 'requesting') return
    cleanupBlob()
    errorMsg.value = null
    status.value = 'requesting'
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const selected = pickMime()
      mime.value = selected || 'audio/webm'
      recorder = selected ? new MediaRecorder(stream, { mimeType: selected }) : new MediaRecorder(stream)
      chunks = []
      recorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunks.push(e.data)
      }
      recorder.onstop = () => {
        blob.value = new Blob(chunks, { type: mime.value })
        objectUrl.value = URL.createObjectURL(blob.value)
        status.value = 'stopped'
        teardownStream()
        if (stopResolver) {
          stopResolver()
          stopResolver = null
        }
      }
      recorder.start()
      startedAt = Date.now()
      durationSec.value = 0
      timerId = window.setInterval(() => {
        durationSec.value = Math.floor((Date.now() - startedAt) / 1000)
      }, 250)
      status.value = 'recording'
    } catch (e) {
      status.value = 'error'
      errorMsg.value = '无法访问麦克风（可能需要授权或浏览器不支持）'
      teardownStream()
    }
  }

  function stop(): Promise<void> {
    return new Promise((resolve) => {
      if (status.value !== 'recording' || !recorder) {
        resolve()
        return
      }
      stopResolver = resolve
      if (timerId !== null) {
        clearInterval(timerId)
        timerId = null
      }
      recorder.stop()
    })
  }

  function cancel() {
    // 先解绑 onstop / ondataavailable，避免 stop 后浏览器异步回调
    // 把刚清空的 blob / status 又写成「已录制」
    if (recorder) {
      recorder.onstop = null
      recorder.ondataavailable = null
      if (status.value === 'recording') {
        try {
          recorder.stop()
        } catch {
          // 忽略已经停止等情况
        }
      }
    }
    if (timerId !== null) {
      clearInterval(timerId)
      timerId = null
    }
    if (stopResolver) {
      stopResolver()
      stopResolver = null
    }
    chunks = []
    teardownStream()
    cleanupBlob()
    status.value = 'idle'
    errorMsg.value = null
    durationSec.value = 0
  }

  function reset() {
    cancel()
  }

  function teardownStream() {
    if (stream) {
      stream.getTracks().forEach((t) => t.stop())
      stream = null
    }
    recorder = null
  }

  function cleanupBlob() {
    if (objectUrl.value) {
      URL.revokeObjectURL(objectUrl.value)
      objectUrl.value = null
    }
    blob.value = null
  }

  return {
    status,
    errorMsg,
    durationSec,
    blob,
    objectUrl,
    mime,
    start,
    stop,
    cancel,
    reset,
  }
}
