<script setup lang="ts">
const props = defineProps<{ modelValue: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: number] }>()

const options = [1, 2, 3, 4, 5]

function select(n: number) {
  if (n !== props.modelValue) emit('update:modelValue', n)
}
</script>

<template>
  <div class="inline-flex rounded-2xl bg-mint-50 p-1 text-sm font-medium">
    <button
      v-for="n in options"
      :key="n"
      type="button"
      class="w-10 rounded-xl py-1.5 transition"
      :class="n === modelValue ? 'bg-white text-mint-600 shadow-sm' : 'text-gray-500 hover:text-mint-600'"
      @click="select(n)"
    >
      {{ n }}
    </button>
  </div>
</template>
