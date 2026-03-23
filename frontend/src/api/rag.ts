import { fetchJson } from '@/composables/useStream'
import type { ApiResponse } from './chat'

const BASE = '/rag'

export interface SearchResult {
  content: string
  score: number
  source: string
}

export const ragApi = {
  addDocument: (content: string, source: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/documents`, {
      method: 'POST',
      body: JSON.stringify({ content, source }),
      headers: { 'Content-Type': 'application/json' },
    }),

  listDocuments: () =>
    fetchJson<ApiResponse<Record<string, number>>>(`${BASE}/documents`),

  deleteDocument: (source: string) =>
    fetchJson<ApiResponse<void>>(`${BASE}/documents/${encodeURIComponent(source)}`, {
      method: 'DELETE',
    }),

  search: (query: string, topK = 5, threshold = 0.5, source?: string) => {
    const params = new URLSearchParams({ query, topK: String(topK), threshold: String(threshold) })
    if (source) params.set('source', source)
    return fetchJson<ApiResponse<SearchResult[]>>(`${BASE}/search?${params}`)
  },

  ask: (question: string) =>
    fetchJson<ApiResponse<string>>(`${BASE}/ask?question=${encodeURIComponent(question)}`),

  askFiltered: (question: string, source: string) =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/ask/filtered?question=${encodeURIComponent(question)}&source=${encodeURIComponent(source)}`,
    ),
}
