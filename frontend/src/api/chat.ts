import { fetchJson, streamFetch } from '@/composables/useStream'

const BASE = '/chat'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface TokenUsageInfo {
  response: string
  promptTokens: number
  generationTokens: number
  totalTokens: number
  durationMs: number
  estimatedCostUsd: number
}

export type ChatPersona = 'ASSISTANT' | 'TEACHER' | 'CODE_REVIEWER' | 'TRANSLATOR' | 'PSYCHOLOGIST'

export const chatApi = {
  simple: (message: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/simple?message=${encodeURIComponent(message)}`),

  stream: (message: string) =>
    streamFetch(`${BASE}/stream?message=${encodeURIComponent(message)}`),

  persona: (message: string, persona: ChatPersona) =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/persona?message=${encodeURIComponent(message)}&persona=${persona}`,
    ),

  streamPersona: (message: string, persona: ChatPersona) =>
    streamFetch(`${BASE}/stream/persona?message=${encodeURIComponent(message)}&persona=${persona}`),

  tokenInfo: (message: string) =>
    fetchJson<ApiResponse<TokenUsageInfo>>(`${BASE}/token-info?message=${encodeURIComponent(message)}`),

  translate: (text: string, targetLanguage = '中文') =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/translate?text=${encodeURIComponent(text)}&targetLanguage=${encodeURIComponent(targetLanguage)}`,
    ),

  codeReview: (code: string, language = 'Java') =>
    fetchJson<ApiResponse<string>>(`${BASE}/code-review`, {
      method: 'POST',
      body: JSON.stringify({ code, language }),
      headers: { 'Content-Type': 'application/json' },
    }),
}
