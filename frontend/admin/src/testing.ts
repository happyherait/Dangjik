import { vi } from 'vitest'

/** 경로별 응답을 정해 둔 가짜 fetch. 테스트에서만 쓴다. */
export function fakeApi(routes: Record<string, { status?: number; body: unknown }>) {
  const calls: { method: string; url: string; headers?: HeadersInit }[] = []
  const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'
    calls.push({ method, url, headers: init?.headers })
    const route = routes[`${method} ${url}`] ?? routes[url]
    if (!route) return new Response(JSON.stringify({ message: 'not found' }), { status: 404 })
    const status = route.status ?? 200
    return new Response(status === 204 ? null : JSON.stringify(route.body), { status })
  })
  vi.stubGlobal('fetch', fetchMock)
  return { calls, fetchMock }
}
