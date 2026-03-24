import { fetchJson, streamFetch } from '@/composables/useStream'
import type { ApiResponse } from './chat'

const BASE = '/telesales'

export interface IntentAnalysis {
  primaryIntent: string
  secondaryIntents: string[]
  sentiment: string
  urgency: number
  priceSensitivity: string
  keyEntities: string[]
  suggestedStrategy: string
  summary: string
}

export interface QualityReport {
  complianceScore: number
  professionalismScore: number
  customerExperienceScore: number
  conversionProbability: number
  violations: string[]
  strengths: string[]
  suggestions: string[]
  overallGrade: string
  summary: string
}

export interface ScriptMatch {
  content: string
  intent: string
  segment: string
  score: number
}

export interface CustomerProfile {
  customerId: string
  name: string
  segment: string
  riskLevel: string
  creditScore: number
  ownedProducts: string[]
  callCount: number
  lastCallResult: string
  notes: string
}

export const telesalesApi = {
  analyzeIntent: (speech: string) =>
    fetchJson<ApiResponse<IntentAnalysis>>(`${BASE}/intent?speech=${encodeURIComponent(speech)}`, {
      method: 'POST',
    }),

  initScripts: () =>
    fetchJson<ApiResponse<string>>(`${BASE}/scripts/init`, { method: 'POST' }),

  addScript: (content: string, intent: string, segment: string) =>
    fetchJson<ApiResponse<void>>(`${BASE}/scripts`, {
      method: 'POST',
      body: JSON.stringify({ content, intent, segment }),
      headers: { 'Content-Type': 'application/json' },
    }),

  recommendScripts: (speech: string, intent?: string, topK = 3) => {
    const params = new URLSearchParams({ speech, topK: String(topK) })
    if (intent) params.set('intent', intent)
    return fetchJson<ApiResponse<ScriptMatch[]>>(`${BASE}/scripts/recommend?${params}`)
  },

  getCustomer: (customerId: string) =>
    fetchJson<ApiResponse<CustomerProfile>>(`${BASE}/customer/${customerId}`),

  dialogue: (conversationId: string, speech: string) =>
    fetchJson<ApiResponse<string>>(
      `${BASE}/dialogue?conversationId=${encodeURIComponent(conversationId)}&speech=${encodeURIComponent(speech)}`,
    ),

  dialogueStream: (conversationId: string, speech: string) =>
    streamFetch(
      `${BASE}/dialogue/stream?conversationId=${encodeURIComponent(conversationId)}&speech=${encodeURIComponent(speech)}`,
    ),

  assessQuality: (transcript: string, agentId = '') =>
    fetchJson<ApiResponse<QualityReport>>(`${BASE}/quality/assess`, {
      method: 'POST',
      body: JSON.stringify({ transcript, agentId }),
      headers: { 'Content-Type': 'application/json' },
    }),

  simulateStream: (conversationId: string, speech: string, intent?: IntentAnalysis) =>
    streamFetch(`${BASE}/simulation/stream`, {
      method: 'POST',
      body: JSON.stringify({ conversationId, speech, intent: intent ?? null }),
      headers: { 'Content-Type': 'application/json' },
    }),
}

export const INTENT_LABELS: Record<string, string> = {
  PRICE_INQUIRY: '💰 价格询问',
  PRODUCT_INQUIRY: '📦 产品咨询',
  REJECTION: '❌ 明确拒绝',
  HESITATION: '🤔 犹豫观望',
  COMPETITOR_COMPARE: '⚖️ 竞品对比',
  CLOSING_SIGNAL: '✅ 成交信号',
  COMPLAINT: '😡 投诉',
  OFF_TOPIC: '💬 偏离主题',
}

export const STRATEGY_LABELS: Record<string, string> = {
  PRICE_ANCHOR: '价格锚定',
  SOCIAL_PROOF: '社会证明',
  SCARCITY: '稀缺制造',
  FEATURE_EMPHASIS: '功能强调',
  EMPATHY: '共情倾听',
  ESCALATE: '转人工',
  CLOSE: '促成成交',
}

export const SENTIMENT_COLORS: Record<string, string> = {
  POSITIVE: 'text-green-600 dark:text-green-400',
  NEUTRAL: 'text-gray-600 dark:text-gray-400',
  NEGATIVE: 'text-red-500 dark:text-red-400',
  FRUSTRATED: 'text-red-700 dark:text-red-300',
  EXCITED: 'text-emerald-600 dark:text-emerald-400',
}
