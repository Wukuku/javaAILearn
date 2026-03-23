import { fetchJson } from '@/composables/useStream'
import type { ApiResponse } from './chat'

const BASE = '/memory'

export interface ConversationStats {
  conversationId: string
  totalMessages: number
  userMessages: number
  assistantMessages: number
  estimatedTokens: number
}

export const memoryApi = {
  chat: (conversationId: string, message: string, historySize = 20) =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/chat?conversationId=${encodeURIComponent(conversationId)}&message=${encodeURIComponent(message)}&historySize=${historySize}`,
    ),

  history: (conversationId: string) =>
    fetchJson<ApiResponse<Array<{ role: string; content: string }>>>(
      `${BASE}/history/${encodeURIComponent(conversationId)}`,
    ),

  stats: (conversationId: string) =>
    fetchJson<ApiResponse<ConversationStats>>(
      `${BASE}/stats/${encodeURIComponent(conversationId)}`,
    ),

  clear: (conversationId: string) =>
    fetchJson<ApiResponse<void>>(`${BASE}/chat/${encodeURIComponent(conversationId)}`, {
      method: 'DELETE',
    }),
}
