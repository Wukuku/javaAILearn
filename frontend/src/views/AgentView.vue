<template>
  <div class="flex flex-col h-full">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-3 px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 shrink-0">
      <span class="text-xs text-gray-500 dark:text-gray-400">模式</span>
      <button
        v-for="tab in tabs"
        :key="tab.value"
        @click="switchTab(tab.value)"
        class="text-xs px-3 py-1 rounded-full transition-colors"
        :class="activeTab === tab.value
          ? 'bg-sky-500 text-white'
          : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50'"
      >
        {{ tab.icon }} {{ tab.label }}
      </button>

      <!-- Stream toggle (for travel & analyze) -->
      <label v-if="activeTab !== 'chat'" class="flex items-center gap-2 cursor-pointer ml-2">
        <div class="relative">
          <input type="checkbox" v-model="streamMode" class="sr-only" />
          <div class="w-9 h-5 rounded-full transition-colors" :class="streamMode ? 'bg-sky-500' : 'bg-gray-300 dark:bg-gray-600'"></div>
          <div class="absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform" :class="streamMode ? 'translate-x-4' : ''"></div>
        </div>
        <span class="text-xs text-gray-500 dark:text-gray-400">流式（看推理过程）</span>
      </label>

      <!-- Conversation ID for chat mode -->
      <div v-if="activeTab === 'chat'" class="flex items-center gap-2 ml-2">
        <span class="text-xs text-gray-500 dark:text-gray-400">会话ID</span>
        <input
          v-model="conversationId"
          placeholder="trip-001"
          class="text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-800 dark:text-white px-3 py-1 w-32 focus:outline-none focus:ring-2 focus:ring-sky-500"
        />
      </div>
    </div>

    <!-- Travel plan / Analyze form -->
    <div v-if="activeTab === 'travel'" class="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shrink-0">
      <div class="flex items-center gap-3">
        <div class="flex-1 flex gap-3">
          <input
            v-model="destination"
            placeholder="目的地，例：成都、东京、巴黎"
            class="flex-1 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            @keydown.enter="planTravel"
          />
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">天数</span>
            <input v-model.number="days" type="number" min="1" max="14" class="w-16 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500" />
          </div>
          <button @click="planTravel" :disabled="!destination || loading" class="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-600 text-white text-sm disabled:opacity-50 transition-colors">
            生成规划
          </button>
        </div>
      </div>
      <div class="flex gap-2 mt-2">
        <button v-for="d in ['成都 3天', '北京 4天', '上海 2天', '西安 3天']" :key="d"
          @click="() => { const [dest, dStr] = d.split(' '); destination = dest; days = parseInt(dStr); planTravel() }"
          :disabled="loading"
          class="text-xs px-2 py-1 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-sky-50 dark:hover:bg-sky-900/20 hover:text-sky-600 transition-colors disabled:opacity-50"
        >{{ d }}</button>
      </div>
    </div>

    <div v-if="activeTab === 'analyze'" class="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 shrink-0">
      <div class="flex gap-3">
        <input
          v-model="analyzeTask"
          placeholder="分析任务，例：比较上海和深圳的城市特点"
          class="flex-1 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          @keydown.enter="doAnalyze"
        />
        <button @click="doAnalyze" :disabled="!analyzeTask || loading" class="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-600 text-white text-sm disabled:opacity-50 transition-colors">
          开始分析
        </button>
      </div>
      <div class="flex gap-2 mt-2">
        <button v-for="task in ['分析北京适合旅游的季节', '比较上海和深圳的城市特点', 'Spring AI vs LangChain4j']" :key="task"
          @click="analyzeTask = task; doAnalyze()"
          :disabled="loading"
          class="text-xs px-2 py-1 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-sky-50 dark:hover:bg-sky-900/20 hover:text-sky-600 transition-colors disabled:opacity-50"
        >{{ task }}</button>
      </div>
    </div>

    <!-- Messages -->
    <div ref="msgContainer" class="flex-1 overflow-y-auto px-4 py-4 space-y-4">
      <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400 dark:text-gray-600">
        <div class="text-5xl mb-3">🤖</div>
        <div class="text-lg font-medium">Agent 自主推理</div>
        <div class="text-sm mt-1">
          <template v-if="activeTab === 'travel'">填写目的地和天数，Agent 将自动调用工具规划行程</template>
          <template v-else-if="activeTab === 'analyze'">输入分析任务，Agent 将多步推理给出深度分析</template>
          <template v-else>使用会话ID进行带记忆的多轮 Agent 对话</template>
        </div>
        <div class="text-xs mt-2 text-gray-300 dark:text-gray-700">推荐开启「流式」模式，实时看到 Agent 推理过程</div>
      </div>
      <MessageBubble v-for="msg in messages" :key="msg.id" :message="msg" />
    </div>

    <!-- Error -->
    <div v-if="error" class="mx-4 mb-2 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm">
      ⚠️ {{ error }}
    </div>

    <!-- Chat input (only for chat mode) -->
    <ChatInput
      v-if="activeTab === 'chat'"
      :disabled="loading"
      @send="handleChat"
      placeholder="与 Agent 多轮对话..."
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import MessageBubble from '@/components/MessageBubble.vue'
import ChatInput from '@/components/ChatInput.vue'
import { agentApi } from '@/api/agent'
import type { Message } from '@/stores/chat'

const messages = ref<Message[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const msgContainer = ref<HTMLDivElement>()

const activeTab = ref<'travel' | 'analyze' | 'chat'>('travel')
const streamMode = ref(true)
const destination = ref('')
const days = ref(3)
const analyzeTask = ref('')
const conversationId = ref('agent-001')

const tabs = [
  { value: 'travel', icon: '✈️', label: '旅游规划' },
  { value: 'analyze', icon: '🔬', label: '研究分析' },
  { value: 'chat', icon: '💬', label: '多轮对话' },
]

function switchTab(tab: 'travel' | 'analyze' | 'chat') {
  activeTab.value = tab
  messages.value = []
  error.value = null
}

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

async function runStream(gen: AsyncGenerator<string>, userLabel: string) {
  addMsg('user', userLabel)
  const aiMsg = addMsg('assistant', '', { streaming: true })
  let content = ''
  for await (const chunk of gen) {
    content += chunk
    updateMsg(aiMsg.id, { content, streaming: true })
    scrollToBottom()
  }
  updateMsg(aiMsg.id, { content, streaming: false })
}

async function planTravel() {
  if (!destination.value) return
  error.value = null
  loading.value = true
  const label = `规划 ${destination.value} ${days.value} 天行程`
  try {
    if (streamMode.value) {
      await runStream(agentApi.streamTravelPlan(destination.value, days.value), label)
    } else {
      addMsg('user', label)
      const res = await agentApi.travelPlan(destination.value, days.value)
      addMsg('assistant', res.data)
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function doAnalyze() {
  if (!analyzeTask.value) return
  error.value = null
  loading.value = true
  const task = analyzeTask.value
  try {
    if (streamMode.value) {
      await runStream(agentApi.streamAnalyze(task), task)
    } else {
      addMsg('user', task)
      const res = await agentApi.analyze(task)
      addMsg('assistant', res.data)
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

async function handleChat(text: string) {
  if (!conversationId.value.trim()) {
    error.value = '请先设置会话ID'
    return
  }
  error.value = null
  addMsg('user', text)
  loading.value = true
  try {
    const res = await agentApi.chat(conversationId.value, text)
    addMsg('assistant', res.data)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}
</script>
