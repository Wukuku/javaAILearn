<template>
  <div class="flex h-full overflow-hidden">
    <!-- Left panel: document management -->
    <div class="w-64 shrink-0 flex flex-col border-r border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
      <div class="px-3 py-2 border-b border-gray-200 dark:border-gray-700 font-medium text-sm text-gray-700 dark:text-gray-300">
        📄 文档管理
      </div>

      <!-- Add document -->
      <div class="p-3 border-b border-gray-200 dark:border-gray-700 space-y-2">
        <input
          v-model="docSource"
          placeholder="来源名称 (source)"
          class="w-full text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-sky-500"
        />
        <textarea
          v-model="docContent"
          placeholder="粘贴文档内容..."
          rows="5"
          class="w-full text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-sky-500 resize-none"
        />
        <button
          @click="addDocument"
          :disabled="!docContent || !docSource || loading"
          class="w-full text-xs px-3 py-1.5 rounded bg-sky-500 hover:bg-sky-600 text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          + 添加到知识库
        </button>
      </div>

      <!-- Sample documents -->
      <div class="p-3 border-b border-gray-200 dark:border-gray-700">
        <div class="text-xs text-gray-500 dark:text-gray-400 mb-2">预置示例文档</div>
        <button
          v-for="sample in sampleDocs"
          :key="sample.source"
          @click="loadSample(sample)"
          class="w-full text-left text-xs px-2 py-1 rounded hover:bg-sky-50 dark:hover:bg-sky-900/20 text-sky-600 dark:text-sky-400 transition-colors"
        >
          📝 {{ sample.source }}
        </button>
      </div>

      <!-- Document list -->
      <div class="flex-1 overflow-y-auto p-3">
        <div class="flex items-center justify-between mb-2">
          <div class="text-xs text-gray-500 dark:text-gray-400">已入库文档</div>
          <button @click="refreshDocs" class="text-xs text-sky-500 hover:text-sky-600">刷新</button>
        </div>
        <div v-if="Object.keys(docList).length === 0" class="text-xs text-gray-400 dark:text-gray-600 text-center py-4">
          暂无文档
        </div>
        <div
          v-for="(count, src) in docList"
          :key="src"
          class="flex items-center justify-between py-1.5 px-2 rounded text-xs hover:bg-gray-100 dark:hover:bg-gray-700 group"
        >
          <span class="text-gray-700 dark:text-gray-300 truncate flex-1">{{ src }}</span>
          <span class="text-gray-400 mr-2">{{ count }} chunks</span>
          <button
            @click="deleteDoc(String(src))"
            class="opacity-0 group-hover:opacity-100 text-red-400 hover:text-red-600 transition-opacity"
          >✕</button>
        </div>
      </div>
    </div>

    <!-- Right panel: search / ask -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Mode tabs -->
      <div class="flex items-center gap-2 px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shrink-0">
        <button
          v-for="tab in ['搜索调试', '知识问答']"
          :key="tab"
          @click="activeTab = tab"
          class="text-xs px-3 py-1 rounded-full transition-colors"
          :class="activeTab === tab
            ? 'bg-sky-500 text-white'
            : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200'"
        >
          {{ tab }}
        </button>
        <div v-if="activeTab === '搜索调试'" class="flex items-center gap-2 ml-4">
          <span class="text-xs text-gray-500">TopK:</span>
          <input v-model.number="topK" type="number" min="1" max="10" class="w-12 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-0.5 focus:outline-none" />
          <span class="text-xs text-gray-500">阈值:</span>
          <input v-model.number="threshold" type="number" min="0" max="1" step="0.1" class="w-14 text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-2 py-0.5 focus:outline-none" />
        </div>
      </div>

      <!-- Search results -->
      <div v-if="activeTab === '搜索调试'" class="flex-1 flex flex-col overflow-hidden">
        <div class="flex-1 overflow-y-auto px-4 py-4 space-y-3">
          <div v-if="searchResults.length === 0 && !loading" class="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-600">
            <div class="text-4xl mb-2">🔍</div>
            <div class="text-sm">输入查询词，直接检索向量库中的相关片段</div>
            <div class="text-xs mt-1">可查看相似度分数，用于调试 RAG 效果</div>
          </div>
          <div
            v-for="(r, i) in searchResults"
            :key="i"
            class="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 text-sm"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-sky-600 dark:text-sky-400">📄 {{ r.source }}</span>
              <span class="text-xs px-2 py-0.5 rounded-full font-mono"
                :class="r.score > 0.8 ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                  : r.score > 0.6 ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
                  : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'"
              >
                相似度 {{ r.score?.toFixed(4) }}
              </span>
            </div>
            <div class="text-gray-700 dark:text-gray-300 text-xs leading-relaxed">{{ r.content }}</div>
          </div>
        </div>
        <div class="border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 px-4 py-3">
          <div class="flex gap-3 max-w-4xl mx-auto">
            <input
              v-model="searchQuery"
              @keydown.enter="doSearch"
              placeholder="输入查询词，直接检索向量库..."
              class="flex-1 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
            <button @click="doSearch" :disabled="!searchQuery || loading" class="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-600 text-white text-sm disabled:opacity-50 transition-colors">
              搜索
            </button>
          </div>
        </div>
      </div>

      <!-- Q&A messages -->
      <div v-else class="flex-1 flex flex-col overflow-hidden">
        <div ref="msgContainer" class="flex-1 overflow-y-auto px-4 py-4 space-y-4">
          <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-600">
            <div class="text-4xl mb-2">📚</div>
            <div class="text-sm">基于知识库内容回答问题</div>
            <div class="text-xs mt-1">先添加文档，再提问</div>
          </div>
          <MessageBubble v-for="msg in messages" :key="msg.id" :message="msg" />
        </div>
        <div v-if="error" class="mx-4 mb-2 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm">
          ⚠️ {{ error }}
        </div>
        <ChatInput :disabled="loading" @send="handleAsk" placeholder="基于知识库提问..." />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import MessageBubble from '@/components/MessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import { ragApi, type SearchResult } from '@/api/rag'
import type { Message } from '@/stores/chat'

const messages = ref<Message[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const activeTab = ref('知识问答')
const msgContainer = ref<HTMLDivElement>()

const docContent = ref('')
const docSource = ref('')
const docList = ref<Record<string, number>>({})

const searchQuery = ref('')
const searchResults = ref<SearchResult[]>([])
const topK = ref(5)
const threshold = ref(0.5)

const sampleDocs = [
  {
    source: 'spring-ai-intro',
    content: 'Spring AI 是 Spring 生态中的 AI 集成框架，支持 OpenAI、Azure OpenAI、Ollama 等多种 AI 提供商。它提供统一的 ChatClient API、向量存储抽象、RAG（检索增强生成）支持以及 Function Calling 功能。Spring AI 1.0.0-M6 是目前最新的里程碑版本，正在向 GA 版本迈进。',
  },
  {
    source: 'vector-store-docs',
    content: '向量存储（VectorStore）是 RAG 系统的核心组件。Spring AI 支持 SimpleVectorStore（内存）、Chroma、Pinecone、Weaviate、Redis 等多种向量数据库。向量存储将文本转换为高维向量，通过余弦相似度或欧氏距离进行语义检索，能够找到语义上相关的文档片段。',
  },
]

function loadSample(sample: typeof sampleDocs[0]) {
  docSource.value = sample.source
  docContent.value = sample.content
}

async function addDocument() {
  if (!docContent.value || !docSource.value) return
  loading.value = true
  error.value = null
  try {
    const res = await ragApi.addDocument(docContent.value, docSource.value)
    alert(res.data)
    docContent.value = ''
    docSource.value = ''
    await refreshDocs()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function refreshDocs() {
  try {
    const res = await ragApi.listDocuments()
    docList.value = res.data
  } catch { /* ignore */ }
}

async function deleteDoc(src: string) {
  if (!confirm(`删除文档来源 "${src}"？`)) return
  try {
    await ragApi.deleteDocument(src)
    await refreshDocs()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}

async function doSearch() {
  if (!searchQuery.value) return
  loading.value = true
  error.value = null
  try {
    const res = await ragApi.search(searchQuery.value, topK.value, threshold.value)
    searchResults.value = res.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function handleAsk(question: string) {
  error.value = null
  const userMsg: Message = { id: crypto.randomUUID(), role: 'user', content: question, timestamp: Date.now() }
  messages.value.push(userMsg)
  loading.value = true
  await nextTick()
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  try {
    const res = await ragApi.ask(question)
    messages.value.push({ id: crypto.randomUUID(), role: 'assistant', content: res.data, timestamp: Date.now() })
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
    await nextTick()
    if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

onMounted(refreshDocs)
</script>
