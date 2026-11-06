export interface Partner {
  partnerId: string
  name: string
  status: string
  commissionRate: number
}

export interface Settlement {
  settlementId: string
  period: string
  grossAmount: number
  commission: number
  netAmount: number
  status: string
}

export interface PartnerDashboard {
  onSale: number
  soldOut: number
  readyToShip: number
  waitingPayment: number
  openClaims: number
  latestSettlement: Settlement | null
}

export interface PartnerProduct {
  productId: string
  name: string
  category: string
  price: number
  stock: number
  status: string
}

export interface PartnerOrderLine {
  orderId: string
  lineNo: number
  productId: string
  productName: string
  qty: number
  unitPrice: number
  shipmentStatus: string
  orderStatus: string
  orderedAt: string
  memberName: string
}

export interface PartnerClaim {
  claimId: string
  orderId: string
  claimType: string
  status: string
  reason: string
  requestedAt: string
}

export interface Member {
  memberId: string
  name: string
  email: string
  grade: string
  joinedAt: string
}

export interface Inquiry {
  inquiryId: string
  memberId: string
  memberName: string
  orderId: string | null
  channel: string
  title: string
  body: string
  status: string
  createdAt: string
}

export interface Member360 {
  member: Member
  orders: { orderId: string; channel: string; status: string; totalAmount: number; orderedAt: string }[]
  inquiries: Inquiry[]
  totalSpent: number
}

export interface Coupon {
  couponId: string
  name: string
  discountRate: number
  validTo: string
  status: string
}

export interface Segment {
  grade: string
  members: number
}
