import { fetchJson } from '@/composables/useStream'
import type { ApiResponse } from './chat'

const BASE = '/tools'

export type ToolType = 'weather' | 'calculate' | 'datetime' | 'multi'

export const toolsApi = {
  weather: (message: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/weather?message=${encodeURIComponent(message)}`),

  calculate: (message: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/calculate?message=${encodeURIComponent(message)}`),

  datetime: (message: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/datetime?message=${encodeURIComponent(message)}`),

  multi: (message: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/multi?message=${encodeURIComponent(message)}`),

  call: (tool: ToolType, message: string): Promise<ApiResponse<string>> => {
    switch (tool) {
      case 'weather': return toolsApi.weather(message)
      case 'calculate': return toolsApi.calculate(message)
      case 'datetime': return toolsApi.datetime(message)
      case 'multi': return toolsApi.multi(message)
    }
  },
}
