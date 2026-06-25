<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { recordsApi, type RecordDto } from '@/api/records'
import RecordCard from '@/components/RecordCard.vue'

const items = ref<RecordDto[]>([])
const total = ref(0)
const page = ref(0)
const size = 20
const loading = ref(false)
const expanded = ref<Set<number>>(new Set())

async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    items.value = []
    page.value = 0
  }
  try {
    const res = await recordsApi.list(page.value, size)
    total.value = res.total
    items.value.push(...res.items)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  load()
}

function toggle(id: number) {
  if (expanded.value.has(id)) expanded.value.delete(id)
  else expanded.value.add(id)
}

onMounted(() => load(true))
</script>

<template>
  <main class="mx-auto max-w-xl px-4 py-8 pb-24">
    <header class="mb-6 flex items-baseline justify-between">
      <div>
        <h1 class="text-xl font-bold text-mint-600">时间轴</h1>
        <p class="text-xs text-gray-500">共 {{ total }} 条记录</p>
      </div>
      <nav class="flex gap-2 text-xs text-gray-500">
        <RouterLink to="/" class="rounded-full px-3 py-1 hover:bg-mint-50">今日</RouterLink>
        <RouterLink to="/random" class="rounded-full px-3 py-1 hover:bg-mint-50">随机</RouterLink>
      </nav>
    </header>

    <div v-if="!loading && !items.length" class="rounded-3xl bg-white p-8 text-center text-sm text-gray-400">
      还没有任何记录，回首页写一条吧
    </div>

    <div class="space-y-3">
      <div v-for="r in items" :key="r.id" @click="toggle(r.id)" class="cursor-pointer">
        <RecordCard :record="r" :mode="expanded.has(r.id) ? 'full' : 'compact'" />
      </div>
    </div>

    <div class="mt-6 text-center">
      <button
        v-if="items.length < total"
        :disabled="loading"
        class="rounded-2xl bg-white px-6 py-2 text-sm text-mint-600 shadow-sm transition hover:bg-mint-50 disabled:opacity-60"
        @click="loadMore"
      >
        {{ loading ? '加载中…' : '加载更多' }}
      </button>
      <p v-else-if="items.length > 0" class="text-xs text-gray-400">没有更多了</p>
    </div>
  </main>
</template>
