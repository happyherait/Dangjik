// backend/runbook 의 PaymentResyncRunbook, PlanHasher, ApprovalRules 를 옮긴 것.
// shared/golden/runbook-cases.json 으로 Java와 같은 결과를 내는지 확인한다.

export interface OrderSnapshot {
  orderId: string
  status: string
  amount: number
}

export interface PgTransaction {
  pgTxId: string
  orderId: string
  status: string
  amount: number
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

export const RUNBOOK_ID = 'PAYMENT_RESYNC'
export const MAX_TARGETS = 20

async function sha256Hex(text: string): Promise<string> {
  const bytes = new TextEncoder().encode(text)
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('')
}

export function planHash(runbookId: string, changes: Change[]): Promise<string> {
  let canonical = `${runbookId}\n`
  for (const c of changes) {
    canonical += `${c.orderId}|${c.fromStatus}|${c.toStatus}|${c.pgTxId}|${c.amount}\n`
  }
  return sha256Hex(canonical)
}

const won = (n: number) => String(n)

export async function plan(
  requestedOrderIds: string[],
  orders: Map<string, OrderSnapshot>,
  pg: PgTransaction[],
): Promise<RunbookPlan> {
  const requested = [...new Set(requestedOrderIds)]
  if (requested.length === 0) throw new Error('대상 주문이 없습니다.')
  if (requested.length > MAX_TARGETS) throw new Error(`한 번에 ${MAX_TARGETS}건까지만 처리할 수 있습니다.`)

  const changes: Change[] = []
  const skipped: Skip[] = []
  for (const orderId of requested) {
    const order = orders.get(orderId)
    if (!order) {
      skipped.push({ orderId, reason: '주문이 없음' })
      continue
    }
    if (order.status !== 'PAYMENT_PENDING') {
      skipped.push({ orderId, reason: `결제대기 상태가 아님(${order.status})` })
      continue
    }
    const approved = pg.filter((t) => t.orderId === orderId && t.status === 'APPROVED')
    if (approved.length === 0) {
      skipped.push({ orderId, reason: 'PG 승인 거래가 없음' })
      continue
    }
    if (approved.length > 1) {
      skipped.push({ orderId, reason: 'PG 승인 거래가 2건 이상이라 수동 확인 필요' })
      continue
    }
    const tx = approved[0]!
    if (tx.amount !== order.amount) {
      skipped.push({ orderId, reason: `금액 불일치(주문 ${won(order.amount)}, PG ${won(tx.amount)})` })
      continue
    }
    changes.push({ orderId, fromStatus: 'PAYMENT_PENDING', toStatus: 'PAID', pgTxId: tx.pgTxId, amount: tx.amount })
  }
  changes.sort((a, b) => (a.orderId < b.orderId ? -1 : a.orderId > b.orderId ? 1 : 0))
  return { runbookId: RUNBOOK_ID, changes, skipped, planHash: await planHash(RUNBOOK_ID, changes) }
}

export function decide(
  requestedBy: string,
  approverId: string,
  approverRole: string,
  approvedHash: string,
  current: RunbookPlan,
): { allowed: boolean; reason: string } {
  if (current.changes.length === 0) return { allowed: false, reason: '실행할 변경이 없음' }
  if (requestedBy === approverId) return { allowed: false, reason: '요청자 본인은 승인할 수 없음' }
  if (approverRole !== 'OPS_LEAD') return { allowed: false, reason: '운영 팀장(OPS_LEAD)만 승인할 수 있음' }
  if (current.planHash !== approvedHash) return { allowed: false, reason: '승인 후 계획이 바뀌어 다시 승인해야 함' }
  return { allowed: true, reason: '승인 가능' }
}
