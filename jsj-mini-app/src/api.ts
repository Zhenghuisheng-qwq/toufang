export async function api<T = unknown>(path: string, data?: unknown): Promise<T> {
  const response = await fetch('/api/h5' + path, {
    method: data === undefined ? 'GET' : 'POST', credentials: 'same-origin', cache: 'no-store',
    headers: data === undefined ? {} : { 'Content-Type': 'application/json', 'X-H5-Request': '1' },
    body: data === undefined ? undefined : JSON.stringify(data), signal: AbortSignal.timeout(20000)
  })
  if (!response.ok) throw new Error('服务暂时不可用，请稍后重试')
  const result = await response.json() as { code: number; msg?: string; data: T }
  if (result.code !== 200) throw new Error(result.msg || '请求未完成，请稍后重试')
  return result.data
}
export function message(error: unknown): string {
  return error instanceof Error ? (error.name === 'TimeoutError' ? '请求超时，请重试；已有订单不会重复创建。' : error.message) : '请求未完成，请稍后重试'
}
export function requestKey(): string {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), value => value.toString(16).padStart(2, '0')).join('')
}
