import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createAppRouter } from './router'
import App from './App.vue'
import { currentStaff } from './staffContext'
import { fakeApi } from './testing'
import type { DutyAnswer, RunView } from './types'

const change = { orderId: 'ORD-20261105-0007', fromStatus: 'PAYMENT_PENDING', toStatus: 'PAID', pgTxId: 'PGTX-88007', amount: 39000 }

const answer: DutyAnswer = {
  mode: 'demo',
  answer: '진단\n- 웹훅을 받지 못했습니다.',
  steps: [
    { tool: 'get_inquiry', input: {}, error: false, summary: 'INQ-0001 문의 조회' },
    { tool: 'propose_payment_resync', input: {}, error: false, summary: 'RUN-1 승인 대기 등록 (대상 1건)' },
  ],
  proposal: { runId: 'RUN-1', created: true, plan: { runbookId: 'PAYMENT_RESYNC', changes: [change], skipped: [], planHash: 'h'.repeat(64) } },
}

const run: RunView = {
  runId: 'RUN-1', runbookId: 'PAYMENT_RESYNC', ticketId: null, requestedBy: 'ops.kim', status: 'WAITING_APPROVAL',
  createdAt: '2026-11-06T10:00:00', executedAt: null, planHash: 'h'.repeat(64),
  proposal: { requestedOrderIds: [change.orderId], reason: '웹훅 누락', changes: [change], skipped: [] },
  currentPlanHash: 'h'.repeat(64), stale: false,
}

async function open(path: string) {
  const router = createAppRouter()
  router.push(path)
  await router.isReady()
  const wrapper = mount(App, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

afterEach(() => {
  vi.unstubAllGlobals()
  currentStaff.value = 'ops.kim'
})

describe('AI 당직', () => {
  it('조회 과정과 답, 런북 제안 링크를 보여준다', async () => {
    const api = fakeApi({
      '/api/ops/chat/mode': { body: { mode: 'demo' } },
      'POST /api/ops/chat': { body: answer },
    })
    const wrapper = await open('/ops/chat')
    expect(wrapper.find('h1').text()).toContain('데모 모드')

    await wrapper.find('textarea').setValue('INQ-0001 확인')
    await wrapper.find('form.ask').trigger('submit')
    await flushPromises()

    expect(wrapper.findAll('.steps li')).toHaveLength(2)
    expect(wrapper.find('.answer').text()).toContain('웹훅을 받지 못했습니다')
    expect(wrapper.find('.proposal a').attributes('href')).toBe('#/ops/runbooks/RUN-1')
    const post = api.calls.find((c) => c.method === 'POST')
    expect(post?.url).toBe('/api/ops/chat')
  })
})

describe('런북 승인', () => {
  it('승인하면 지금 고른 직원과 화면에 보이는 계획 해시로 요청하고, 거절 이유를 보여준다', async () => {
    const api = fakeApi({
      '/api/ops/runbook-runs/RUN-1': { body: run },
      'POST /api/ops/runbook-runs/RUN-1/approve': { status: 409, body: { message: '요청자 본인은 승인할 수 없음' } },
    })
    const wrapper = await open('/ops/runbooks/RUN-1')

    expect(wrapper.text()).toContain('결제대기 → 결제완료')
    await wrapper.find('.decide button').trigger('click')
    await flushPromises()

    const fetchMock = api.fetchMock
    const call = fetchMock.mock.calls.find((c) => (c[1] as RequestInit | undefined)?.method === 'POST')
    expect(JSON.parse(String((call?.[1] as RequestInit).body))).toEqual({ approverId: 'ops.kim', planHash: 'h'.repeat(64) })
    expect(wrapper.find('[role="status"]').text()).toBe('요청자 본인은 승인할 수 없음')
  })

  it('사실이 바뀐 제안은 경고를 보여준다', async () => {
    fakeApi({ '/api/ops/runbook-runs/RUN-1': { body: { ...run, stale: true } } })
    const wrapper = await open('/ops/runbooks/RUN-1')

    expect(wrapper.text()).toContain('제안 이후 주문이나 PG 데이터가 바뀌었습니다')
  })
})

describe('앱 버전 관리', () => {
  it('지금 고른 직원으로 저장하고, 거절되면 이유를 보여준다', async () => {
    const api = fakeApi({
      '/api/ops/app-versions': {
        body: [{ platform: 'ANDROID', latestVersion: '3.8.0', minSupportedVersion: '3.5.0', updatedAt: '2026-10-28T10:00:00' }],
      },
      'PUT /api/ops/app-versions/ANDROID': { status: 409, body: { message: '운영 팀장(OPS_LEAD)만 앱 버전 기준을 바꿀 수 있습니다.' } },
    })
    const wrapper = await open('/ops/app-versions')

    await wrapper.find('input[aria-label="ANDROID 최소 지원 버전"]').setValue('3.8.0')
    await wrapper.find('tbody button').trigger('click')
    await flushPromises()

    const call = api.fetchMock.mock.calls.find((c) => (c[1] as RequestInit | undefined)?.method === 'PUT')
    expect(JSON.parse(String((call?.[1] as RequestInit).body))).toEqual({
      latestVersion: '3.8.0',
      minSupportedVersion: '3.8.0',
      userId: 'ops.kim',
    })
    expect(wrapper.find('[role="status"]').text()).toContain('운영 팀장')
  })
})
