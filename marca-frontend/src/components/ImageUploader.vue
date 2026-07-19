<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { recordsApi, type ImageDto } from '@/api/records'
import { resolveMediaUrl } from '@/lib/mediaUrl'

const props = defineProps<{
  modelValue: ImageDto[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ImageDto[]]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const errorMsg = ref<string | null>(null)
const items = ref<ImageDto[]>([...props.modelValue])

watch(
  () => props.modelValue,
  (v) => {
    items.value = [...v]
  },
  { deep: true },
)

function emitChange() {
  emit(
    'update:modelValue',
    items.value.map((it, i) => ({ ...it, sortOrder: i })),
  )
}

function pickFiles() {
  fileInput.value?.click()
}

async function onFiles(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const files = Array.from(input.files)
  input.value = '' // 允许再次选同一文件
  errorMsg.value = null
  uploading.value = true
  try {
    for (const file of files) {
      const res = await recordsApi.uploadImage(file)
      items.value.push({
        url: res.imageUrl,
        width: res.width,
        height: res.height,
        bytes: res.bytes,
      })
    }
    emitChange()
  } catch (err) {
    errorMsg.value = axios.isAxiosError(err)
      ? (err.response?.data?.message ?? '上传失败')
      : '上传失败'
  } finally {
    uploading.value = false
  }
}

function removeAt(i: number) {
  items.value.splice(i, 1)
  emitChange()
}
</script>

<template>
  <section class="rounded-3xl bg-white p-5 shadow-sm">
    <header class="mb-3 flex items-center gap-2 text-xs text-mint-600">
      <span class="rounded-full bg-mint-100 px-2 py-0.5">图片</span>
      <span class="text-gray-400">截图、照片、表情包都行</span>
    </header>

    <div class="grid grid-cols-3 gap-2">
      <div
        v-for="(img, i) in items"
        :key="img.url"
        class="group relative aspect-square overflow-hidden rounded-2xl bg-mint-50"
      >
        <img :src="resolveMediaUrl(img.url) ?? undefined" :alt="`图片 ${i + 1}`" class="h-full w-full object-cover" />
        <button
          type="button"
          class="absolute right-1 top-1 hidden h-6 w-6 items-center justify-center rounded-full bg-white/90 text-xs text-red-500 shadow group-hover:flex"
          @click="removeAt(i)"
          title="删除"
        >
          ✕
        </button>
      </div>

      <button
        type="button"
        class="flex aspect-square items-center justify-center rounded-2xl border-2 border-dashed border-mint-200 bg-mint-50/40 text-2xl text-mint-400 transition hover:border-mint-400 hover:text-mint-600 disabled:opacity-60"
        :disabled="uploading"
        @click="pickFiles"
      >
        {{ uploading ? '…' : '+' }}
      </button>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/png,image/jpeg,image/webp,image/gif"
      multiple
      class="hidden"
      @change="onFiles"
    />

    <p v-if="errorMsg" class="mt-2 text-xs text-red-500">{{ errorMsg }}</p>
  </section>
</template>
