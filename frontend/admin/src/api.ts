import { ref, watchEffect, type Ref } from 'vue'

export class ApiFailure extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

async function readError(res: Response): Promise<ApiFailure> {
  let message = `요청이 실패했습니다 (${res.status})`
  try {
    const body = (await res.json()) as { message?: string }
    if (body.message) message = body.message
  } catch {
    // 본문이 JSON이 아니면 기본 메시지를 쓴다
  }
  return new ApiFailure(res.status, message)
}

export async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(path)
  if (!res.ok) throw await readError(res)
  return (await res.json()) as T
}

export async function post(path: string, user: string): Promise<void> {
  const res = await fetch(path, { method: 'POST', headers: { 'X-Dangjik-User': user } })
  if (!res.ok) throw await readError(res)
}

export async function postJson<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw await readError(res)
  return (await res.json()) as T
}

/** 경로가 바뀌면 다시 불러오는 조회. reload()로 직접 다시 부를 수도 있다. */
export function useJson<T>(path: () => string) {
  const data = ref<T>() as Ref<T | undefined>
  const error = ref<string>()
  const loading = ref(false)

  async function load() {
    loading.value = true
    error.value = undefined
    try {
      data.value = await getJson<T>(path())
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
    } finally {
      loading.value = false
    }
  }

  watchEffect(() => {
    path()
    void load()
  })

  return { data, error, loading, reload: load }
}
