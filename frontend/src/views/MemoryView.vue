<template>
  <div class="flex flex-col h-full">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-3 px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 shrink-0">
      <span class="text-xs text-gray-500 dark:text-gray-400">会话ID</span>
      <input
        v-model="conversationId"
        placeholder="e.g. user-001"
        class="text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-3 py-1 w-36 focus:outline-none focus:ring-2 focus:ring-sky-500"
      />
      <button @click="loadHistory" :disabled="!conversationId || loading" class="btn-outline text-xs">📜 历史</button>
      <button @click="loadStats" :disabled="!conversationId || loading" class="btn-outline text-xs">📊 统计</button>
      <button @click="clearMemory" :disabled="!conversationId || loading" class="btn-outline text-xs text-red-500 dark:text-red-400 border-red-300 dark:border-red-700 hover:bg-red-50 dark:hover:bg-red-900/20">🗑 清除</button>
      <span class="text-xs text-gray-400 dark:text-gray-600 ml-auto">不同 conversationId 之间记忆完全隔离</span>
    </div>

    <!-- Stats panel -->
    <div v-if="stats" class="px-4 py-2 bg-sky-50 dark:bg-sky-900/20 border-b border-sky-100 dark:border-sky-800 shrink-0">
      <div class="flex gap-6 text-xs text-sky-800 dark:text-sky-300">
        <span>会话: <b>{{ stats.conversationId }}</b></span>
        <span>总消息: <b>{{ stats.totalMessages }}</b></span>
        <span>用户: <b>{{ stats.userMessages }}</b></span>
        <span>AI: <b>{{ stats.assistantMessages }}</b></span>
        <span>估算 Token: <b>{{ stats.estimatedTokens }}</b></span>
      </div>
    </div>

    <!-- Messages -->
    <div ref="msgContainer" class="flex-1 overflow-y-auto px-4 py-4 space-y-4">
      <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-600">
        <div class="text-5xl mb-3">🧠</div>
        <div class="text-lg font-medium">多轮对话记忆</div>
        <div class="text-sm mt-1">设置会话ID，开始对话，AI 会记住上下文</div>
        <div class="text-xs mt-3 text-gray-300 dark:text-gray-700">
          提示：用相同 conversationId 多次对话，换一个 ID 则记忆重置
        </div>
      </div>
      <MessageBubble v-for="msg in messages" :key="msg.id" :message="msg" />
    </div>

    <!-- Error -->
    <div v-if="error" class="mx-4 mb-2 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm">
      ⚠️ {{ error }}
    </div>

    <ChatInput :disabled="loading" @send="handleSend" placeholder="输入消息（需先设置会话ID）..." />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import MessageBubble from '@/components/MessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import { memoryApi, type ConversationStats } from '@/api/memory'
import type { Message } from '@/stores/chat'

const messages = ref<Message[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const conversationId = ref('user-001')
const stats = ref<ConversationStats | null>(null)
const msgContainer = ref<HTMLDivElement>()

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

async function handleSend(text: string) {
  if (!conversationId.value.trim()) {
    error.value = '请先设置会话ID'
    return
  }
  error.value = null
  stats.value = null
  addMsg('user', text)
  loading.value = true
  try {
    const res = await memoryApi.chat(conversationId.value, text)
    addMsg('assistant', res.data)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  if (!conversationId.value) return
  loading.value = true
  error.value = null
  try {
    const res = await memoryApi.history(conversationId.value)
    messages.value = []
    for (const h of res.data) {
      messages.value.push({
        id: crypto.randomUUID(),
        role: h.role as 'user' | 'assistant',
        content: h.content,
        timestamp: Date.now(),
      })
    }
    scrollToBottom()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  if (!conversationId.value) return
  loading.value = true
  error.value = null
  try {
    const res = await memoryApi.stats(conversationId.value)
    stats.value = res.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function clearMemory() {
  if (!conversationId.value) return
  if (!confirm(`确认清除会话 "${conversationId.value}" 的所有记忆？`)) return
  loading.value = true
  error.value = null
  try {
    await memoryApi.clear(conversationId.value)
    messages.value = []
    stats.value = null
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.btn-outline {
  @apply px-3 py-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed;
}
</style>
