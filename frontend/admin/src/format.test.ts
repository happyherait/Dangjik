import { describe, expect, it } from 'vitest'
import { dateTime, label, maskName, stockLevel, won } from './format'

describe('표시 규칙', () => {
  it('입점사에게는 고객 이름 가운데를 가린다', () => {
    expect(maskName('김하루')).toBe('김*루')
    expect(maskName('이봄')).toBe('이*')
    expect(maskName('남궁하늘')).toBe('남**늘')
    expect(maskName('김')).toBe('*')
  })

  it('금액, 날짜, 상태 코드를 읽기 쉽게 바꾼다', () => {
    expect(won(3704800)).toBe('3,704,800원')
    expect(dateTime('2026-11-05T21:14:00')).toBe('2026-11-05 21:14')
    expect(label('WAITING_PAYMENT')).toBe('결제대기')
    expect(label('NEW_CODE')).toBe('NEW_CODE')
  })

  it('재고 수준을 나눈다', () => {
    expect(stockLevel(0)).toBe('out')
    expect(stockLevel(7)).toBe('low')
    expect(stockLevel(10)).toBe('ok')
  })
})
