// 몰 화면에서 쓰는 순수 함수. 화면과 분리해 테스트한다.

export interface Product {
  productId: string
  partnerId: string
  name: string
  category: string
  price: number
  stock: number
  description: string
  status: string
}

export interface OrderItem {
  lineNo: number
  productId: string
  productName: string
  qty: number
  unitPrice: number
  shipmentStatus: string
}

export interface Order {
  orderId: string
  memberId: string
  channel: 'WEB' | 'APP'
  status: string
  totalAmount: number
  orderedAt: string
  items: OrderItem[]
}

export function formatPrice(won: number): string {
  return `${won.toLocaleString('ko-KR')}원`
}

const ORDER_STATUS: Record<string, string> = {
  PAYMENT_PENDING: '결제 확인 중',
  PAID: '결제 완료',
  SHIPPING: '배송 중',
  DELIVERED: '배송 완료',
  CANCELED: '주문 취소',
}

export function orderStatusLabel(status: string): string {
  return ORDER_STATUS[status] ?? status
}

export function isSoldOut(product: Pick<Product, 'status' | 'stock'>): boolean {
  return product.status === 'SOLD_OUT' || product.stock <= 0
}

export function categoriesOf(products: Product[]): string[] {
  return [...new Set(products.map((p) => p.category))]
}

export function filterByCategory(products: Product[], category?: string): Product[] {
  return category ? products.filter((p) => p.category === category) : products
}

const CATEGORY_ICON: Record<string, string> = {
  과일: '🍎',
  채소: '🥬',
  침구: '🛏️',
  가구: '🪑',
  주방: '☕',
  베이커리: '🥐',
}

export function categoryIcon(category: string): string {
  return CATEGORY_ICON[category] ?? '📦'
}

export const ORDER_ID_PATTERN = /^ORD-\d{8}-\d{4}$/
