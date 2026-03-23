<template>
  <div class="border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 px-4 py-3">
    <div class="flex items-end gap-3 max-w-4xl mx-auto">
      <div class="flex-1 relative">
        <textarea
          ref="inputRef"
          v-model="text"
          :placeholder="placeholder"
          :disabled="disabled"
          rows="1"
          class="w-full resize-none rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 px-4 py-3 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          style="min-height: 48px; max-height: 200px; overflow-y: auto;"
          @input="autoResize"
          @keydown.enter.exact.prevent="handleSend"
          @keydown.enter.shift.exact="() => {}"
        />
      </div>
      <button
        @click="handleSend"
        :disabled="disabled || !text.trim()"
        class="shrink-0 w-11 h-11 rounded-xl bg-sky-500 hover:bg-sky-600 disabled:bg-gray-200 dark:disabled:bg-gray-700 disabled:cursor-not-allowed text-white disabled:text-gray-400 dark:disabled:text-gray-500 flex items-center justify-center transition-colors"
        title="发送 (Enter)"
      >
        <svg v-if="!disabled" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
        <!-- Loading spinner -->
        <svg v-else class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
        </svg>
      </button>
    </div>
    <div class="text-xs text-gray-400 dark:text-gray-600 text-center mt-1">
      Enter 发送 · Shift+Enter 换行
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'

const props = defineProps<{
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
}>()

const text = ref('')
const inputRef = ref<HTMLTextAreaElement>()

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

async function handleSend() {
  if (props.disabled || !text.value.trim()) return
  const msg = text.value.trim()
  text.value = ''
  await nextTick()
  autoResize()
  emit('send', msg)
}

defineExpose({ focus: () => inputRef.value?.focus() })
</script>
