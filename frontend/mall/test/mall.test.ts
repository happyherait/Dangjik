import { describe, expect, it } from 'vitest'
import {
  categoriesOf,
  filterByCategory,
  formatPrice,
  isSoldOut,
  ORDER_ID_PATTERN,
  orderStatusLabel,
  type Product,
  shipmentStatusLabel,
} from '../app/utils/mall'

const product = (over: Partial<Product>): Product => ({
  productId: 'PRD-0001',
  partnerId: 'P-001',
  name: '제철 사과 3kg',
  category: '과일',
  price: 39000,
  stock: 10,
  description: '',
  status: 'ON_SALE',
  ...over,
})

describe('몰 화면 규칙', () => {
  it('가격은 원 단위로 쉼표를 넣는다', () => {
    expect(formatPrice(39000)).toBe('39,000원')
  })

  it('결제대기는 고객에게 결제 확인 중으로 보인다', () => {
    expect(orderStatusLabel('PAYMENT_PENDING')).toBe('결제 확인 중')
    expect(orderStatusLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('배송 상태는 한글로 보인다', () => {
    expect(shipmentStatusLabel('WAITING_PAYMENT')).toBe('결제 대기')
    expect(shipmentStatusLabel('DELIVERED')).toBe('배송 완료')
    expect(shipmentStatusLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('품절 상태이거나 재고가 0이면 품절', () => {
    expect(isSoldOut(product({ status: 'SOLD_OUT', stock: 5 }))).toBe(true)
    expect(isSoldOut(product({ stock: 0 }))).toBe(true)
    expect(isSoldOut(product({}))).toBe(false)
  })

  it('카테고리는 중복 없이 나오고 선택한 카테고리만 거른다', () => {
    const list = [product({ category: '과일' }), product({ category: '채소' }), product({ category: '과일' })]
    expect(categoriesOf(list)).toEqual(['과일', '채소'])
    expect(filterByCategory(list, '채소')).toHaveLength(1)
    expect(filterByCategory(list)).toHaveLength(3)
  })

  it('주문번호 형식을 검사한다', () => {
    expect(ORDER_ID_PATTERN.test('ORD-20261105-0007')).toBe(true)
    expect(ORDER_ID_PATTERN.test('ORD-1')).toBe(false)
  })
})
