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

export interface Change {
  orderId: string
  fromStatus: string
  toStatus: string
  pgTxId: string
  amount: number
}

export interface Skip {
  orderId: string
  reason: string
}

export interface RunbookPlan {
  runbookId: string
  changes: Change[]
  skipped: Skip[]
  planHash: string
}

export interface Proposal {
  plan: RunbookPlan
  runId: string | null
  created: boolean
}

export interface RunView {
  runId: string
  runbookId: string
  ticketId: string | null
  requestedBy: string
  status: string
  createdAt: string
  executedAt: string | null
  planHash: string
  proposal: { requestedOrderIds: string[]; reason: string; changes: Change[]; skipped: Skip[] }
  currentPlanHash: string | null
  stale: boolean
}

export interface DutyStep {
  tool: string
  input: Record<string, unknown>
  error: boolean
  summary: string
}

export interface DutyAnswer {
  mode: string
  answer: string
  steps: DutyStep[]
  proposal: Proposal | null
}

export interface Ticket {
  ticketId: string
  area: string
  severity: string
  title: string
  description: string | null
  status: string
  inquiryId: string | null
  createdBy: string
  createdAt: string
}

export interface TimelineEvent {
  at: string
  kind: string
  level: string
  title: string
  detail: string | null
}

export interface Deployment {
  deployId: string
  area: string
  version: string
  note: string
  deployedBy: string
  deployedAt: string
}

export interface BatchJob {
  jobId: string
  name: string
  lastStatus: string
  lastMessage: string
  lastRunAt: string | null
}

export interface AppVersion {
  platform: string
  latestVersion: string
  minSupportedVersion: string
  updatedAt: string
}

export interface AuditEvent {
  eventId: number
  actorId: string
  action: string
  target: string
  detail: string | null
  createdAt: string
}
