import { fetchJson } from '@/composables/useStream'
import type { ApiResponse } from './chat'

const BASE = '/structured'

export interface BookInfo {
  title: string
  author: string
  year: number
  genre: string
  summary: string
  rating: number
}

export interface MovieInfo {
  title: string
  director: string
  year: number
  rating: number
  summary: string
}

export interface ResumeInfo {
  name: string
  email?: string
  phone?: string
  summary?: string
  skills: string[]
  experience: Array<{
    company: string
    position: string
    duration: string
    description?: string
  }>
  education: Array<{
    school: string
    degree: string
    major?: string
    year?: string
  }>
}

export const structuredApi = {
  book: (title: string) =>
    fetchJson<ApiResponse<BookInfo>>(`${BASE}/book?title=${encodeURIComponent(title)}`),

  movies: (genre: string, count = 3) =>
    fetchJson<ApiResponse<MovieInfo[]>>(
      `${BASE}/movies?genre=${encodeURIComponent(genre)}&count=${count}`,
    ),

  resume: (resumeText: string) =>
    fetchJson<ApiResponse<ResumeInfo>>(`${BASE}/resume`, {
      method: 'POST',
      body: JSON.stringify({ resumeText }),
      headers: { 'Content-Type': 'application/json' },
    }),
}
