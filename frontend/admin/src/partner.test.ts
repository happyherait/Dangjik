import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createAppRouter } from './router'
import App from './App.vue'
import { currentPartnerId } from './partnerContext'
import { fakeApi } from './testing'

const partners = [
  { partnerId: 'P-001', name: '오늘농장', status: 'ACTIVE', commissionRate: 12 },
  { partnerId: 'P-002', name: '모노리빙', status: 'ACTIVE', commissionRate: 15 },
]

const lines = [
  {
    orderId: 'ORD-20261105-0007', lineNo: 1, productId: 'PRD-0001', productName: '제철 사과 3kg', qty: 1,
    unitPrice: 39000, shipmentStatus: 'WAITING_PAYMENT', orderStatus: 'PAYMENT_PENDING',
    orderedAt: '2026-11-05T21:14:00', memberName: '김하루',
  },
  {
    orderId: 'ORD-20261105-0004', lineNo: 1, productId: 'PRD-0002', productName: '유기농 방울토마토 1kg', qty: 1,
    unitPrice: 12000, shipmentStatus: 'READY', orderStatus: 'PAID',
    orderedAt: '2026-11-05T18:30:00', memberName: '박여름',
  },
]

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
  currentPartnerId.value = 'P-001'
})

describe('파트너 포털 주문/출고', () => {
  it('고객 이름을 가리고, 결제 확인된 주문에만 출고 버튼을 보여준다', async () => {
    fakeApi({ '/api/partner': { body: partners }, '/api/partner/P-001/orders': { body: lines } })
    const wrapper = await open('/partner/orders')

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    expect(rows[0].text()).toContain('김*루')
    expect(rows[0].text()).not.toContain('김하루')
    expect(rows[0].text()).toContain('결제 확인 후 출고')
    expect(rows[0].find('button').exists()).toBe(false)
    expect(rows[1].find('button').text()).toBe('출고')
  })

  it('출고하면 입점사 사용자로 요청하고, 서버가 거절하면 그 이유를 보여준다', async () => {
    const api = fakeApi({
      '/api/partner': { body: partners },
      '/api/partner/P-001/orders': { body: lines },
      'POST /api/partner/P-001/orders/ORD-20261105-0004/lines/1/ship': {
        status: 409,
        body: { message: '출고 준비 상태가 아닙니다.' },
      },
    })
    const wrapper = await open('/partner/orders')

    await wrapper.find('tbody tr:nth-child(2) button').trigger('click')
    await flushPromises()

    const post = api.calls.find((c) => c.method === 'POST')
    expect(post?.headers).toEqual({ 'X-Dangjik-User': 'p001.han' })
    expect(wrapper.find('[role="status"]').text()).toBe('출고 준비 상태가 아닙니다.')
  })

  it('입점사를 바꾸면 그 입점사의 데이터를 다시 불러온다', async () => {
    const api = fakeApi({
      '/api/partner': { body: partners },
      '/api/partner/P-001/orders': { body: lines },
      '/api/partner/P-002/orders': { body: [] },
    })
    const wrapper = await open('/partner/orders')

    await wrapper.find('select').setValue('P-002')
    await flushPromises()

    expect(api.calls.some((c) => c.url === '/api/partner/P-002/orders')).toBe(true)
    expect(wrapper.text()).toContain('주문이 없습니다.')
  })
})
