import { detectApp } from '~/utils/mall'

interface NativeBridge {
  getAppInfo(): string
  share(text: string): void
}

/** 앱 셸(Android, iOS)이 열어 준 window.DangjikApp. 브라우저에서는 없다 */
export function useAppBridge() {
  const app = useState<ReturnType<typeof detectApp>>('dangjik-app', () => null)

  onMounted(() => {
    app.value = detectApp(navigator.userAgent)
  })

  function bridge(): NativeBridge | undefined {
    return (window as unknown as { DangjikApp?: NativeBridge }).DangjikApp
  }

  function share(text: string): boolean {
    const b = bridge()
    if (!b) return false
    b.share(text)
    return true
  }

  return { app, share }
}
