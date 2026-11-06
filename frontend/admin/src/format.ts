// 관리자 화면 공통 표시 규칙

export function won(amount: number): string {
  return `${amount.toLocaleString('ko-KR')}원`
}

export function dateTime(iso: string): string {
  return iso.replace('T', ' ').slice(0, 16)
}

/** 입점사에게는 고객 이름 가운데를 가린다. 김하루 → 김*루, 이봄 → 이* */
export function maskName(name: string): string {
  const chars = [...name]
  if (chars.length <= 1) return '*'
  if (chars.length === 2) return `${chars[0]}*`
  return chars[0] + '*'.repeat(chars.length - 2) + chars[chars.length - 1]
}

const LABELS: Record<string, string> = {
  // 주문
  PAYMENT_PENDING: '결제대기',
  PAID: '결제완료',
  SHIPPING: '배송중',
  DELIVERED: '배송완료',
  CANCELED: '취소',
  // 출고
  WAITING_PAYMENT: '결제대기',
  READY: '출고준비',
  // 상품
  ON_SALE: '판매중',
  SOLD_OUT: '품절',
  // 클레임
  CANCEL: '취소',
  RETURN: '반품',
  EXCHANGE: '교환',
  REQUESTED: '접수',
  DONE: '완료',
  // 정산
  CONFIRMED: '확정',
  // 문의
  OPEN: '처리중',
  CLOSED: '완료',
  ESCALATED: 'SR 등록',
  FIXED: '해결',
  // 런북
  WAITING_APPROVAL: '승인 대기',
  EXECUTED: '실행됨',
  REJECTED: '반려',
  FAILED: '실패',
  SUCCESS: '성공',
  WARNING: '경고',
  // SR
  MALL: '몰',
  PARTNER: '파트너',
  CRM: 'CRM',
  HIGH: '높음',
  MEDIUM: '보통',
  LOW: '낮음',
  // 공통
  ACTIVE: '사용중',
  WEB: '웹',
  APP: '앱',
}

export function label(code: string): string {
  return LABELS[code] ?? code
}

export const LOW_STOCK = 10

export function stockLevel(stock: number): 'out' | 'low' | 'ok' {
  if (stock <= 0) return 'out'
  if (stock < LOW_STOCK) return 'low'
  return 'ok'
}
