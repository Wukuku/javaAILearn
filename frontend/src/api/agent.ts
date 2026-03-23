import { fetchJson, streamFetch } from '@/composables/useStream'
import type { ApiResponse } from './chat'

const BASE = '/agent'

export const agentApi = {
  travelPlan: (destination: string, days = 3) =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/travel-plan?destination=${encodeURIComponent(destination)}&days=${days}`,
    ),

  streamTravelPlan: (destination: string, days = 3) =>
    streamFetch(`${BASE}/travel-plan/stream?destination=${encodeURIComponent(destination)}&days=${days}`),

  chat: (conversationId: string, message: string) =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/chat?conversationId=${encodeURIComponent(conversationId)}&message=${encodeURIComponent(message)}`,
    ),

  analyze: (task: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/analyze?task=${encodeURIComponent(task)}`),

  streamAnalyze: (task: string) =>
    streamFetch(`${BASE}/analyze/stream?task=${encodeURIComponent(task)}`),
}
