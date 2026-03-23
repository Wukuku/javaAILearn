<template>
  <div class="flex flex-col h-full">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-3 px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 shrink-0">
      <!-- Persona selector -->
      <div class="flex items-center gap-2">
        <span class="text-xs text-gray-500 dark:text-gray-400">角色</span>
        <select
          v-model="persona"
          class="text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-1 focus:outline-none focus:ring-2 focus:ring-sky-500"
        >
          <option v-for="p in personas" :key="p.value" :value="p.value">{{ p.label }}</option>
        </select>
      </div>

      <!-- Stream toggle -->
      <label class="flex items-center gap-2 cursor-pointer">
        <div class="relative">
          <input type="checkbox" v-model="streamMode" class="sr-only" />
          <div class="w-9 h-5 rounded-full transition-colors" :class="streamMode ? 'bg-sky-500' : 'bg-gray-300 dark:bg-gray-600'"></div>
          <div class="absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform" :class="streamMode ? 'translate-x-4' : ''"></div>
        </div>
        <span class="text-xs text-gray-500 dark:text-gray-400">流式输出</span>
      </label>

      <!-- Mode tabs -->
      <div class="flex gap-1 ml-auto">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          @click="activeTab = tab.value"
          class="text-xs px-3 py-1 rounded-full transition-colors"
          :class="activeTab === tab.value
            ? 'bg-sky-500 text-white'
            : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-600'"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Translate / Code Review forms -->
    <div v-if="activeTab === 'translate'" class="px-4 py-2 bg-amber-50 dark:bg-amber-900/20 border-b border-amber-200 dark:border-amber-800 shrink-0">
      <div class="flex items-center gap-3 text-sm">
        <span class="text-amber-700 dark:text-amber-400 font-medium">翻译模式</span>
        <span class="text-gray-500 dark:text-gray-400">目标语言：</span>
        <select v-model="targetLang" class="rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-0.5 text-xs">
          <option>中文</option><option>英语</option><option>日语</option><option>韩语</option><option>法语</option><option>德语</option>
        </select>
        <span class="text-xs text-gray-400">在输入框输入要翻译的文本</span>
      </div>
    </div>
    <div v-if="activeTab === 'code-review'" class="px-4 py-2 bg-purple-50 dark:bg-purple-900/20 border-b border-purple-200 dark:border-purple-800 shrink-0">
      <div class="flex items-center gap-3 text-sm">
        <span class="text-purple-700 dark:text-purple-400 font-medium">代码审查模式</span>
        <span class="text-gray-500 dark:text-gray-400">语言：</span>
        <select v-model="codeLang" class="rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-0.5 text-xs">
          <option>Java</option><option>Python</option><option>JavaScript</option><option>TypeScript</option><option>Go</option>
        </select>
        <span class="text-xs text-gray-400">在输入框粘贴代码</span>
      </div>
    </div>

    <!-- Messages -->
    <div ref="msgContainer" class="flex-1 overflow-y-auto px-4 py-4 space-y-4">
      <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-600">
        <div class="text-5xl mb-3">💬</div>
        <div class="text-lg font-medium">基础聊天模块</div>
        <div class="text-sm mt-1">支持角色扮演、流式输出、翻译、代码审查</div>
      </div>
      <MessageBubble v-for="msg in messages" :key="msg.id" :message="msg" />

      <!-- Token info display -->
      <div v-if="tokenInfo" class="mx-auto max-w-sm bg-sky-50 dark:bg-sky-900/20 border border-sky-200 dark:border-sky-800 rounded-xl p-3 text-xs text-sky-800 dark:text-sky-300">
        <div class="font-semibold mb-1">📊 Token 统计</div>
        <div class="grid grid-cols-2 gap-x-4 gap-y-0.5">
          <span>Prompt Tokens:</span><span class="font-mono">{{ tokenInfo.promptTokens }}</span>
          <span>Generation Tokens:</span><span class="font-mono">{{ tokenInfo.generationTokens }}</span>
          <span>Total Tokens:</span><span class="font-mono">{{ tokenInfo.totalTokens }}</span>
          <span>耗时:</span><span class="font-mono">{{ tokenInfo.durationMs }}ms</span>
          <span>估算费用:</span><span class="font-mono">${{ tokenInfo.estimatedCostUsd?.toFixed(6) }}</span>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-if="error" class="mx-4 mb-2 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm">
      ⚠️ {{ error }}
    </div>

    <ChatInput :disabled="loading" @send="handleSend" placeholder="输入消息，Enter 发送..." />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import MessageBubble from '@/components/MessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import { chatApi, type TokenUsageInfo, type ChatPersona } from '@/api/chat'
import { useChatStore, type Message } from '@/stores/chat'

const store = useChatStore()
const messages = ref<Message[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const tokenInfo = ref<TokenUsageInfo | null>(null)
const msgContainer = ref<HTMLDivElement>()

const persona = ref<ChatPersona>('ASSISTANT')
const streamMode = ref(true)
const activeTab = ref<'chat' | 'translate' | 'code-review' | 'token'>('chat')
const targetLang = ref('中文')
const codeLang = ref('Java')

const personas = [
  { value: 'ASSISTANT', label: '🤖 通用助手' },
  { value: 'TEACHER', label: '👨‍🏫 老师' },
  { value: 'CODE_REVIEWER', label: '🔍 代码评审' },
  { value: 'TRANSLATOR', label: '🌐 翻译官' },
  { value: 'PSYCHOLOGIST', label: '🧘 心理咨询' },
]

const tabs = [
  { value: 'chat', label: '聊天' },
  { value: 'translate', label: '翻译' },
  { value: 'code-review', label: '代码审查' },
  { value: 'token', label: 'Token统计' },
]

function addMsg(role: 'user' | 'assistant', content: string, extra?: Partial<Message>): Message {
  const msg: Message = { id: crypto.randomUUID(), role, content, timestamp: Date.now(), ...extra }
  messages.value.push(msg)
  scrollToBottom()
  return msg
}

function updateMsg(id: string, patch: Partial<Message>) {
  const idx = messages.value.findIndex(m => m.id === id)
  if (idx !== -1) messages.value[idx] = { ...messages.value[idx], ...patch }
}

async function scrollToBottom() {
  await nextTick()
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

async function handleSend(text: string) {
  error.value = null
  tokenInfo.value = null
  addMsg('user', text)
  loading.value = true

  try {
    if (activeTab.value === 'translate') {
      const res = await chatApi.translate(text, targetLang.value)
      addMsg('assistant', res.data)
    } else if (activeTab.value === 'code-review') {
      const res = await chatApi.codeReview(text, codeLang.value)
      addMsg('assistant', res.data)
    } else if (activeTab.value === 'token') {
      const res = await chatApi.tokenInfo(text)
      addMsg('assistant', res.data.response)
      tokenInfo.value = res.data
    } else if (streamMode.value) {
      // Streaming
      const aiMsg = addMsg('assistant', '', { streaming: true })
      let content = ''
      const gen = persona.value !== 'ASSISTANT'
        ? chatApi.streamPersona(text, persona.value)
        : chatApi.stream(text)
      for await (const chunk of gen) {
        content += chunk
        updateMsg(aiMsg.id, { content, streaming: true })
        scrollToBottom()
      }
      updateMsg(aiMsg.id, { content, streaming: false })
    } else {
      const res = persona.value !== 'ASSISTANT'
        ? await chatApi.persona(text, persona.value)
        : await chatApi.simple(text)
      addMsg('assistant', res.data)
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>
