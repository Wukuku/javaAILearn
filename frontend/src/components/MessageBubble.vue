<template>
  <div
    class="flex gap-3 animate-fade-in"
    :class="message.role === 'user' ? 'flex-row-reverse' : 'flex-row'"
  >
    <!-- Avatar -->
    <div
      class="w-8 h-8 rounded-full flex items-center justify-center text-sm shrink-0 font-medium"
      :class="message.role === 'user'
        ? 'bg-sky-500 text-white'
        : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'"
    >
      {{ message.role === 'user' ? '你' : '🤖' }}
    </div>

    <!-- Bubble -->
    <div
      class="max-w-[75%] rounded-2xl px-4 py-3 text-sm leading-relaxed relative group"
      :class="message.role === 'user'
        ? 'bg-sky-500 text-white rounded-tr-sm'
        : 'bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-200 rounded-tl-sm'"
    >
      <!-- User message: plain text -->
      <template v-if="message.role === 'user'">
        <span class="whitespace-pre-wrap">{{ message.content }}</span>
      </template>

      <!-- AI message: rendered markdown -->
      <template v-else>
        <div
          class="prose-ai"
          v-html="renderedContent"
        />
        <!-- Streaming cursor -->
        <span
          v-if="message.streaming"
          class="inline-block w-2 h-4 bg-sky-500 dark:bg-sky-400 ml-0.5 animate-blink align-middle"
        />
      </template>

      <!-- Timestamp (visible on hover) -->
      <div
        class="absolute -bottom-5 text-xs text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap"
        :class="message.role === 'user' ? 'right-0' : 'left-0'"
      >
        {{ formatTime(message.timestamp) }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import type { Message } from '@/stores/chat'

const props = defineProps<{ message: Message }>()

// Configure marked with syntax highlighting (marked v12 API)
marked.use({
  breaks: true,
  renderer: {
    code({ text, lang }: { text: string; lang?: string }) {
      const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
      const highlighted = hljs.highlight(text, { language }).value
      return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`
    },
  },
})

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  return marked(props.message.content) as string
})

function formatTime(ts: number): string {
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>
