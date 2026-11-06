import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { areas, findMenuItem } from './menu'
import { createAppRouter, routes } from './router'
import App from './App.vue'
import { fakeApi } from './testing'

describe('메뉴 구성', () => {
  it('파트너 포털, CRM, 운영 콘솔 세 영역이 있다', () => {
    expect(areas.map((a) => a.title)).toEqual(['파트너 포털', 'CRM', '운영 콘솔'])
  })

  it('메뉴 경로는 중복되지 않고 모두 라우트가 있다', () => {
    const paths = areas.flatMap((a) => a.items.map((i) => i.path))
    expect(new Set(paths).size).toBe(paths.length)
    for (const path of paths) {
      expect(routes.some((r) => r.path === path)).toBe(true)
      expect(path.startsWith(`/${findArea(path)}/`)).toBe(true)
    }
  })

  it('운영 콘솔에 AI 당직과 런북 승인 메뉴가 있다', () => {
    expect(findMenuItem('/ops/chat')?.title).toBe('AI 당직')
    expect(findMenuItem('/ops/runbooks')?.title).toBe('런북 실행/승인')
  })
})

describe('화면', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('영역을 열면 그 영역의 사이드 메뉴가 보인다', async () => {
    fakeApi({ '/api/crm/members': { body: [] } })
    const router = createAppRouter()
    router.push('/crm/members')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('aside').attributes('aria-label')).toBe('CRM 메뉴')
    expect(wrapper.find('aside').text()).toContain('쿠폰/프로모션')
    expect(wrapper.find('h1').text()).toBe('회원 360')
  })

  it('아직 만들지 않은 메뉴는 준비 중으로 표시한다', async () => {
    const router = createAppRouter()
    router.push('/ops/chat')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.find('aside').text()).toContain('준비 중')
    expect(wrapper.find('.planned').exists()).toBe(true)
  })
})

function findArea(path: string) {
  return areas.find((a) => a.items.some((i) => i.path === path))?.key
}
