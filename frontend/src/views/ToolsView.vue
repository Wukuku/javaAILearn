<template>
  <div class="flex flex-col h-full">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-2 px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 shrink-0">
      <span class="text-xs text-gray-500 dark:text-gray-400 mr-1">工具</span>
      <button
        v-for="t in tools"
        :key="t.value"
        @click="activeTool = t.value"
        class="text-xs px-3 py-1 rounded-full transition-colors"
        :class="activeTool === t.value
          ? 'bg-sky-500 text-white'
          : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50'"
      >
        {{ t.icon }} {{ t.label }}
      </button>

      <!-- Quick examples -->
      <div class="ml-auto flex items-center gap-2">
        <span class="text-xs text-gray-400">快速示例：</span>
        <button
          v-for="ex in currentExamples"
          :key="ex"
          @click="quickSend(ex)"
          class="text-xs px-2 py-1 rounded bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors max-w-[150px] truncate"
          :title="ex"
        >
          {{ ex }}
        </button>
      </div>
    </div>

    <!-- Tool description -->
    <div class="px-4 py-2 bg-amber-50 dark:bg-amber-900/20 border-b border-amber-100 dark:border-amber-800 shrink-0 text-xs text-amber-700 dark:text-amber-400">
      {{ currentToolDesc }}
    </div>

    <!-- Messages -->
    <div ref="msgContainer" class="flex-1 overflow-y-auto px-4 py-4 space-y-4">
      <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-600">
        <div class="text-5xl mb-3">🔧</div>
        <div class="text-lg font-medium">Function Calling</div>
        <div class="text-sm mt-1">模型自主决定调用哪些工具来回答问题</div>
      </div>
      <MessageBubble v-for="msg in messages" :key="msg.id" :message="msg" />
    </div>

    <!-- Error -->
    <div v-if="error" class="mx-4 mb-2 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm">
      ⚠️ {{ error }}
    </div>

    <ChatInput :disabled="loading" @send="handleSend" :placeholder="currentPlaceholder" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import MessageBubble from '@/components/MessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import { toolsApi, type ToolType } from '@/api/tools'
import type { Message } from '@/stores/chat'

const messages = ref<Message[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const activeTool = ref<ToolType>('weather')
const msgContainer = ref<HTMLDivElement>()

const tools = [
  { value: 'weather' as ToolType, icon: '🌤️', label: '天气' },
  { value: 'calculate' as ToolType, icon: '🧮', label: '计算' },
  { value: 'datetime' as ToolType, icon: '🕐', label: '时间' },
  { value: 'multi' as ToolType, icon: '⚡', label: '多工具' },
]

const toolConfig = {
  weather: {
    desc: '调用天气工具查询城市天气，模型会自动解析城市名并调用工具',
    placeholder: '例：北京今天天气怎么样？',
    examples: ['北京今天天气怎么样？', '上海和广州哪个更热？'],
  },
  calculate: {
    desc: '调用计算器工具进行精确数学运算，解决大模型计算不准确的问题',
    placeholder: '例：1234567 乘以 8901234 等于多少？',
    examples: ['1234567 × 8901234 等于多少？', '圆周率乘以100的平方'],
  },
  datetime: {
    desc: '调用时间工具获取当前时间，解决大模型不知道当前时间的问题',
    placeholder: '例：现在北京时间几点？',
    examples: ['现在北京时间几点？', '今天是星期几？'],
  },
  multi: {
    desc: '同时注册多个工具，模型根据用户意图自主决定调用哪些工具（可多次调用）',
    placeholder: '例：北京天气怎样？现在几点了？再帮我算下 365×24',
    examples: ['北京天气怎样？现在几点了？', '帮我查上海天气，再算一下 999×888'],
  },
}

const currentToolDesc = computed(() => toolConfig[activeTool.value].desc)
const currentPlaceholder = computed(() => toolConfig[activeTool.value].placeholder)
const currentExamples = computed(() => toolConfig[activeTool.value].examples)

function addMsg(role: 'user' | 'assistant', content: string): Message {
  const msg: Message = { id: crypto.randomUUID(), role, content, timestamp: Date.now() }
  messages.value.push(msg)
  scrollToBottom()
  return msg
}

async function scrollToBottom() {
  await nextTick()
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

async function send(text: string) {
  error.value = null
  addMsg('user', text)
  loading.value = true
  try {
    const res = await toolsApi.call(activeTool.value, text)
    addMsg('assistant', res.data)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

const handleSend = (text: string) => send(text)
const quickSend = (text: string) => { if (!loading.value) send(text) }
</script>
