import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface Message {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  streaming?: boolean
  timestamp: number
  metadata?: Record<string, unknown>
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const darkMode = ref(false)

  function addMessage(msg: Omit<Message, 'id' | 'timestamp'>): Message {
    const message: Message = {
      ...msg,
      id: crypto.randomUUID(),
      timestamp: Date.now(),
    }
    messages.value.push(message)
    return message
  }

  function updateMessage(id: string, patch: Partial<Message>) {
    const idx = messages.value.findIndex(m => m.id === id)
    if (idx !== -1) {
      messages.value[idx] = { ...messages.value[idx], ...patch }
    }
  }

  function clearMessages() {
    messages.value = []
  }

  function setError(msg: string | null) {
    error.value = msg
  }

  function toggleDarkMode() {
    darkMode.value = !darkMode.value
    if (darkMode.value) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  return {
    messages,
    loading,
    error,
    darkMode,
    addMessage,
    updateMessage,
    clearMessages,
    setError,
    toggleDarkMode,
  }
})
