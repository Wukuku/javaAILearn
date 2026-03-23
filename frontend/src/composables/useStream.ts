/**
 * SSE / ReadableStream utility for streaming text responses.
 */

export async function* streamFetch(
  url: string,
  options?: RequestInit,
): AsyncGenerator<string> {
  const res = await fetch(url, options)

  if (!res.ok) {
    const errText = await res.text()
    throw new Error(`HTTP ${res.status}: ${errText}`)
  }

  const contentType = res.headers.get('content-type') || ''

  // SSE (text/event-stream)
  if (contentType.includes('event-stream')) {
    const reader = res.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() ?? ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data && data !== '[DONE]') {
            yield data
          }
        }
      }
    }
    // flush remaining
    if (buffer.startsWith('data:')) {
      const data = buffer.slice(5).trim()
      if (data && data !== '[DONE]') yield data
    }
  } else {
    // Plain text streaming
    const reader = res.body!.getReader()
    const decoder = new TextDecoder()
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      yield decoder.decode(value, { stream: true })
    }
  }
}

/**
 * Fetch JSON response (non-streaming).
 */
export async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
    ...options,
  })
  if (!res.ok) {
    const errText = await res.text()
    throw new Error(`HTTP ${res.status}: ${errText}`)
  }
  return res.json()
}

/**
 * Fetch plain text response (non-streaming).
 */
export async function fetchText(url: string, options?: RequestInit): Promise<string> {
  const res = await fetch(url, options)
  if (!res.ok) {
    const errText = await res.text()
    throw new Error(`HTTP ${res.status}: ${errText}`)
  }
  return res.text()
}
