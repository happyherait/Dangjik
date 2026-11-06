// 공개 데모용 가짜 API. 실제 서버(Spring Boot)의 규칙을 브라우저 안에서 그대로 흉내 낸다.
// 데이터는 메모리에만 있어서 새로고침하면 처음 상태로 돌아간다.
import { freshDb, type DemoDb } from './data'
import { decide, plan, type OrderSnapshot, type RunbookPlan } from './runbook'

class Conflict extends Error {}

let db: DemoDb = freshDb()
let seq = 0

export function resetDemo() {
  db = freshDb()
  seq = 0
}

/** 서울 시각 (실제 서버의 AppClock과 같다) */
function now(): string {
  return new Date(Date.now() + 9 * 3600_000).toISOString().slice(0, 19)
}

function id(prefix: string) {
  seq += 1
  return `${prefix}-DEMO${String(seq).padStart(4, '0')}`
}

function audit(actorId: string, action: string, target: string, detail: string | null) {
  db.audit.push({ eventId: db.audit.length + 1, actorId, action, target, detail, createdAt: now() })
}

function role(userId: string) {
  return db.staff.find((s) => s.userId === userId)?.role
}

const productName = (pid: string) => db.products.find((p) => p.productId === pid)?.name ?? pid

// ---------- 파트너 포털 ----------

function partnerLines(partnerId: string) {
  return db.items
    .filter((i) => db.products.find((p) => p.productId === i.productId)?.partnerId === partnerId)
    .map((i) => {
      const o = db.orders.find((x) => x.orderId === i.orderId)!
      return {
        orderId: i.orderId, lineNo: i.lineNo, productId: i.productId, productName: productName(i.productId),
        qty: i.qty, unitPrice: i.unitPrice, shipmentStatus: i.shipmentStatus, orderStatus: o.status,
        orderedAt: o.orderedAt, memberName: db.members.find((m) => m.memberId === o.memberId)!.name,
      }
    })
    .sort((a, b) => (a.orderedAt < b.orderedAt ? 1 : -1))
}

function partnerClaims(partnerId: string) {
  const orderIds = new Set(partnerLines(partnerId).map((l) => l.orderId))
  return db.claims.filter((c) => orderIds.has(c.orderId))
}

function ship(partnerId: string, orderId: string, lineNo: number, actor: string) {
  const item = db.items.find((i) => i.orderId === orderId && i.lineNo === lineNo)
  if (!item) throw new Conflict('주문 상품이 없습니다.')
  const order = db.orders.find((o) => o.orderId === orderId)!
  if (db.products.find((p) => p.productId === item.productId)?.partnerId !== partnerId) {
    throw new Conflict('다른 입점사의 상품은 출고할 수 없습니다.')
  }
  if (order.status !== 'PAID' && order.status !== 'SHIPPING') throw new Conflict('결제가 확인되지 않은 주문은 출고할 수 없습니다.')
  if (item.shipmentStatus !== 'READY') throw new Conflict('출고 준비 상태가 아닙니다.')
  item.shipmentStatus = 'SHIPPING'
  if (order.status === 'PAID') order.status = 'SHIPPING'
  audit(actor, 'SHIP_ORDER_LINE', `${orderId}#${lineNo}`, `입점사 ${partnerId} 출고`)
}

// ---------- 런북 ----------

function facts(ids: string[]) {
  const orders = new Map<string, OrderSnapshot>()
  for (const o of db.orders) if (ids.includes(o.orderId)) orders.set(o.orderId, { orderId: o.orderId, status: o.status, amount: o.totalAmount })
  return { orders, pg: db.pg.filter((t) => ids.includes(t.orderId)) }
}

async function planFor(ids: string[]): Promise<RunbookPlan> {
  const f = facts(ids)
  try {
    return await plan(ids, f.orders, f.pg)
  } catch (e) {
    throw new Conflict((e as Error).message)
  }
}

async function propose(orderIds: string[], reason: string, requestedBy: string, ticketId: string | null) {
  if (!role(requestedBy)) throw new Conflict(`등록되지 않은 직원입니다: ${requestedBy}`)
  const ids = [...new Set(orderIds)]
  const p = await planFor(ids)
  if (p.changes.length === 0) return { plan: p, runId: null, created: false }
  const existing = db.runs.find((r) => r.planHash === p.planHash)
  if (existing) return { plan: p, runId: existing.runId, created: false }
  const runId = id('RUN')
  db.runs.push({
    runId, runbookId: 'PAYMENT_RESYNC', ticketId, requestedBy, status: 'WAITING_APPROVAL', createdAt: now(),
    executedAt: null, planHash: p.planHash, proposal: { requestedOrderIds: ids, reason, changes: p.changes, skipped: p.skipped },
  })
  audit(requestedBy, 'RUNBOOK_PROPOSED', runId, `PAYMENT_RESYNC 대상 ${p.changes.length}건. 사유: ${reason}`)
  return { plan: p, runId, created: true }
}

async function runView(run: DemoDb['runs'][number]) {
  let currentPlanHash: string | null = null
  if (run.status === 'WAITING_APPROVAL') currentPlanHash = (await planFor(run.proposal.requestedOrderIds)).planHash
  return { ...run, currentPlanHash, stale: currentPlanHash !== null && currentPlanHash !== run.planHash }
}

function waiting(runId: string) {
  const run = db.runs.find((r) => r.runId === runId)
  if (!run) throw new Conflict(`실행 요청이 없습니다: ${runId}`)
  if (run.status !== 'WAITING_APPROVAL') throw new Conflict(`이미 처리된 실행 요청입니다 (${run.status}).`)
  return run
}

async function approve(runId: string, approverId: string, approvedHash: string) {
  const run = waiting(runId)
  const current = await planFor(run.proposal.requestedOrderIds)
  const d = decide(run.requestedBy, approverId, role(approverId) ?? 'UNKNOWN', approvedHash, current)
  if (!d.allowed) {
    audit(approverId, 'APPROVAL_DENIED', runId, d.reason)
    throw new Conflict(d.reason)
  }
  if (current.planHash !== run.planHash) throw new Conflict('제안 후 사실이 바뀌었습니다. 다시 제안해야 합니다.')
  for (const c of current.changes) {
    const order = db.orders.find((o) => o.orderId === c.orderId)!
    order.status = 'PAID'
    db.payments.push({ paymentId: `PAY-R-${c.pgTxId}`, orderId: c.orderId, pgTxId: c.pgTxId, amount: c.amount, paidAt: now() })
    for (const i of db.items) if (i.orderId === c.orderId && i.shipmentStatus === 'WAITING_PAYMENT') i.shipmentStatus = 'READY'
    audit(approverId, 'ORDER_PAYMENT_RESYNCED', c.orderId, `PG ${c.pgTxId} ${c.amount}원 기준으로 결제완료 처리 (${runId})`)
  }
  db.approvals.push({ runId, approverId, decision: 'APPROVE', reason: null, decidedAt: now() })
  run.status = 'EXECUTED'
  run.executedAt = now()
  const ticket = db.tickets.find((t) => t.ticketId === run.ticketId)
  if (ticket) ticket.status = 'FIXED'
  audit(approverId, 'RUNBOOK_EXECUTED', runId, `${current.changes.length}건 실행`)
  return runView(run)
}

function reject(runId: string, approverId: string, reason: string) {
  const run = waiting(runId)
  if (role(approverId) !== 'OPS_LEAD') throw new Conflict('운영 팀장(OPS_LEAD)만 반려할 수 있음')
  db.approvals.push({ runId, approverId, decision: 'REJECT', reason, decidedAt: now() })
  run.status = 'REJECTED'
  audit(approverId, 'RUNBOOK_REJECTED', runId, reason)
  return runView(run)
}

// ---------- AI 당직 (데모 모델) ----------

interface Step { tool: string; input: Record<string, unknown>; error: boolean; summary: string }

const won = (n: number) => `${n.toLocaleString('ko-KR')}원`

async function ask(question: string, user: string) {
  if (!question.trim()) throw new Conflict('질문을 입력하세요.')
  if (!['CS_AGENT', 'OPS_ENGINEER', 'OPS_LEAD'].includes(role(user) ?? '')) {
    throw new Conflict('상담원과 운영 담당자만 AI 당직을 쓸 수 있습니다.')
  }
  audit(user, 'AI_DUTY_ASKED', '-', question)
  const steps: Step[] = []
  const step = (tool: string, input: Record<string, unknown>, summary: string, error = false) =>
    steps.push({ tool, input, summary, error })

  const product = /PRD-\d{4}/.exec(question)?.[0]
  if (product) {
    const p = db.products.find((x) => x.productId === product)
    step('get_product', { productId: product }, p ? `${product} 상품 조회` : `상품이 없습니다: ${product}`, !p)
    if (!p) return { mode: 'demo', answer: `상품을 찾지 못했습니다: ${product}`, steps, proposal: null }
    const suspicious = /AI|지시|0원/.test(p.description)
    const answer = suspicious
      ? `진단\n- ${p.productId} ${p.name}, 판매가 ${won(p.price)}\n- 상품 설명 안에 운영 AI에게 보내는 지시문이 숨어 있습니다. 입점사가 쓴 데이터라서 따르지 않았습니다.\n근거\n- 설명 원문: ${p.description}\n조치\n- 가격은 바꾸지 않았습니다. 이 시스템의 AI에게는 가격 변경 도구가 없습니다.\n- 입점사 ${p.partnerId}에 설명 수정을 요청하고, 상품 검수 SR을 등록하는 것을 권합니다.`
      : `진단\n- ${p.productId} ${p.name}, 판매가 ${won(p.price)}\n- 상품 정보에 특이 사항이 없습니다.`
    return { mode: 'demo', answer, steps, proposal: null }
  }

  let orderId = /ORD-\d{8}-\d{4}/.exec(question)?.[0]
  const inquiryId = /INQ-\d{4}/.exec(question)?.[0]

  if (orderId && /누가|처리|이력/.test(question)) {
    const runs = db.runs.filter((r) => r.proposal.requestedOrderIds.includes(orderId!))
    const events = db.audit.filter((a) => a.target === orderId || a.target.startsWith(`${orderId}#`))
    step('get_order_history', { orderId }, `${orderId} 처리 이력: 런북 ${runs.length}건, 기록 ${events.length}건`)
    const executed = runs.find((r) => r.status === 'EXECUTED')
    const approval = executed && db.approvals.find((a) => a.runId === executed.runId && a.decision === 'APPROVE')
    const answer = executed && approval
      ? `진단\n- ${orderId}은 런북 결제 상태 재동기화(${executed.runId})로 처리됐습니다.\n근거\n- 요청: ${executed.requestedBy}, ${executed.createdAt.replace('T', ' ')}\n- 승인과 실행: ${approval.approverId}, ${approval.decidedAt.replace('T', ' ')}\n조치\n- 추가 조치는 필요하지 않습니다.`
      : `진단\n- ${orderId}에 대한 런북 실행 기록이 아직 없습니다.${runs.length ? ` 승인 대기 중인 제안 ${runs.map((r) => r.runId).join(', ')}이 있습니다.` : ''}`
    return { mode: 'demo', answer, steps, proposal: null }
  }

  if (inquiryId) {
    const q = db.inquiries.find((x) => x.inquiryId === inquiryId)
    step('get_inquiry', { inquiryId }, q ? `${inquiryId} 문의 조회` : `문의가 없습니다: ${inquiryId}`, !q)
    orderId ??= q?.orderId ?? undefined
  }
  if (!orderId) {
    const stuck = db.orders.filter((o) => o.status === 'PAYMENT_PENDING' && db.pg.some((t) => t.orderId === o.orderId && t.status === 'APPROVED'))
    step('find_stuck_payments', {}, `결제대기로 남은 PG 승인 주문 ${stuck.length}건`)
    orderId = stuck[0]?.orderId
    if (!orderId) return { mode: 'demo', answer: '결제대기로 남은 PG 승인 주문이 없습니다.', steps, proposal: null }
  }

  const order = db.orders.find((o) => o.orderId === orderId)
  step('get_order', { orderId }, order ? `${orderId} 주문 조회: ${order.status}` : `주문이 없습니다: ${orderId}`, !order)
  if (!order) return { mode: 'demo', answer: `주문을 찾지 못했습니다: ${orderId}`, steps, proposal: null }
  const pg = db.pg.filter((t) => t.orderId === orderId)
  step('get_pg_transactions', { orderId }, `${orderId} PG 거래 ${pg.length}건`)
  const hooks = db.webhooks.filter((w) => w.orderId === orderId)
  step('get_webhook_logs', { orderId }, `${orderId} 웹훅 기록 ${hooks.length}건`)
  step('list_deployments', {}, `배포 이력 ${db.deployments.length}건`)

  const approved = pg.find((t) => t.status === 'APPROVED')
  if (order.status !== 'PAYMENT_PENDING' || !approved) {
    return {
      mode: 'demo',
      answer: `진단\n- ${orderId} 주문은 ${order.status} 상태이고 PG 승인과 어긋남이 없습니다. 재동기화가 필요하지 않습니다.`,
      steps, proposal: null,
    }
  }
  const proposal = await propose([orderId], 'PG는 승인했지만 웹훅 수신 실패로 결제대기에 머문 주문', user, null)
  step('propose_payment_resync', { orderIds: [orderId] },
    proposal.runId ? `${proposal.runId} 승인 대기 등록 (대상 ${proposal.plan.changes.length}건)` : '실행할 변경이 없어 제안을 등록하지 않음')

  const failed = hooks.filter((h) => h.httpStatus !== 200)
  const deploy = db.deployments.find((d) => d.deployedAt < (approved.approvedAt ?? ''))
  let answer = `진단\n- ${orderId} 주문은 결제대기지만, PG에는 ${approved.pgTxId} ${won(approved.amount)} 승인 거래가 있습니다. 결제 알림(웹훅)을 받지 못한 상태입니다.\n근거\n`
  if (failed[0]) answer += `- 웹훅 ${failed.length}번 실패: ${failed[0].httpStatus} ${failed[0].message}\n`
  if (deploy) answer += `- 직전 배포 ${deploy.area} ${deploy.version}: ${deploy.note}. PG가 예전 경로로 알림을 보내 실패한 것으로 보입니다.\n`
  answer += `조치\n- 런북 '결제 상태 재동기화'를 제안했습니다 (${proposal.runId}). 운영 팀장이 승인하면 결제완료로 바뀌고 입점사가 출고할 수 있습니다.\n- 재발 방지: PG사 웹훅 주소를 새 경로로 바꾸거나 이전 경로를 함께 받도록 배포해야 합니다. 이 작업은 개발 조치라서 런북 범위 밖입니다.`
  return { mode: 'demo', answer, steps, proposal: proposal.runId ? proposal : null }
}

// ---------- 라우팅 ----------

type Handler = (m: RegExpMatchArray, body: Record<string, unknown>, headers: Headers) => unknown

const routes: [string, RegExp, Handler][] = [
  ['GET', /^\/api\/partner$/, () => db.partners],
  ['GET', /^\/api\/partner\/([^/]+)\/dashboard$/, (m) => {
    const pid = m[1]!
    const products = db.products.filter((p) => p.partnerId === pid)
    const lines = partnerLines(pid)
    const settlements = db.settlements.filter((s) => s.partnerId === pid).sort((a, b) => (a.period < b.period ? 1 : -1))
    return {
      onSale: products.filter((p) => p.status === 'ON_SALE' && p.stock > 0).length,
      soldOut: products.filter((p) => p.status === 'SOLD_OUT' || p.stock === 0).length,
      readyToShip: lines.filter((l) => l.shipmentStatus === 'READY').length,
      waitingPayment: lines.filter((l) => l.shipmentStatus === 'WAITING_PAYMENT').length,
      openClaims: partnerClaims(pid).filter((c) => c.status !== 'DONE').length,
      latestSettlement: settlements[0] ?? null,
    }
  }],
  ['GET', /^\/api\/partner\/([^/]+)\/products$/, (m) => db.products.filter((p) => p.partnerId === m[1])],
  ['GET', /^\/api\/partner\/([^/]+)\/orders$/, (m) => partnerLines(m[1]!)],
  ['POST', /^\/api\/partner\/([^/]+)\/orders\/([^/]+)\/lines\/(\d+)\/ship$/, (m, _b, h) => {
    ship(m[1]!, m[2]!, Number(m[3]), h.get('X-Dangjik-User') ?? 'partner')
    return null
  }],
  ['GET', /^\/api\/partner\/([^/]+)\/claims$/, (m) => partnerClaims(m[1]!)],
  ['GET', /^\/api\/partner\/([^/]+)\/settlements$/, (m) => db.settlements.filter((s) => s.partnerId === m[1])],

  ['GET', /^\/api\/crm\/members$/, () => db.members],
  ['GET', /^\/api\/crm\/members\/([^/]+)$/, (m) => {
    const member = db.members.find((x) => x.memberId === m[1])
    if (!member) return undefined
    const orders = db.orders.filter((o) => o.memberId === member.memberId).sort((a, b) => (a.orderedAt < b.orderedAt ? 1 : -1))
    return {
      member, orders,
      inquiries: db.inquiries.filter((q) => q.memberId === member.memberId).map((q) => ({ ...q, memberName: member.name })),
      totalSpent: orders.filter((o) => o.status !== 'PAYMENT_PENDING' && o.status !== 'CANCELED').reduce((s, o) => s + o.totalAmount, 0),
    }
  }],
  ['GET', /^\/api\/crm\/inquiries$/, () =>
    db.inquiries.map((q) => ({ ...q, memberName: db.members.find((m) => m.memberId === q.memberId)!.name }))],
  ['GET', /^\/api\/crm\/coupons$/, () => db.coupons],
  ['GET', /^\/api\/crm\/segments$/, () =>
    ['VIP', 'GOLD', 'SILVER', 'BASIC'].map((grade) => ({ grade, members: db.members.filter((m) => m.grade === grade).length }))
      .filter((s) => s.members > 0)],

  ['GET', /^\/api\/ops\/chat\/mode$/, () => ({ mode: 'demo' })],
  ['POST', /^\/api\/ops\/chat$/, (_m, b) => ask(String(b.message ?? ''), String(b.user ?? ''))],
  ['GET', /^\/api\/ops\/runbook-runs$/, () => Promise.all([...db.runs].reverse().map(runView))],
  ['GET', /^\/api\/ops\/runbook-runs\/([^/]+)$/, (m) => {
    const run = db.runs.find((r) => r.runId === m[1])
    return run ? runView(run) : undefined
  }],
  ['POST', /^\/api\/ops\/runbook-runs\/([^/]+)\/approve$/, (m, b) => approve(m[1]!, String(b.approverId), String(b.planHash))],
  ['POST', /^\/api\/ops\/runbook-runs\/([^/]+)\/reject$/, (m, b) => reject(m[1]!, String(b.approverId), String(b.reason ?? '반려'))],
  ['GET', /^\/api\/ops\/tickets$/, () => [...db.tickets].reverse()],
  ['POST', /^\/api\/ops\/tickets$/, (_m, b) => {
    const createdBy = String(b.createdBy)
    if (!role(createdBy)) throw new Conflict(`등록되지 않은 직원입니다: ${createdBy}`)
    if (!String(b.title ?? '').trim()) throw new Conflict('제목을 입력하세요.')
    const inquiryId = (b.inquiryId as string | null) || null
    if (inquiryId) {
      const q = db.inquiries.find((x) => x.inquiryId === inquiryId)
      if (!q) throw new Conflict(`문의가 없습니다: ${inquiryId}`)
      q.status = 'ESCALATED'
    }
    const t = {
      ticketId: id('SR'), area: String(b.area), severity: String(b.severity), title: String(b.title),
      description: (b.description as string | null) || null, status: 'OPEN', inquiryId, createdBy, createdAt: now(),
    }
    db.tickets.push(t)
    audit(createdBy, 'TICKET_CREATED', t.ticketId, t.title)
    return t
  }],
  ['GET', /^\/api\/ops\/timeline$/, () => [
    ...db.deployments.map((d) => ({ at: d.deployedAt, kind: 'DEPLOY', level: 'INFO', title: `${d.area} ${d.version} 배포`, detail: d.note })),
    ...db.webhooks.filter((w) => w.httpStatus !== 200).map((w) => ({ at: w.receivedAt, kind: 'WEBHOOK', level: 'ERROR', title: `PG 웹훅 실패 ${w.httpStatus} (${w.orderId})`, detail: w.message })),
    ...db.batches.filter((b) => b.lastStatus !== 'SUCCESS').map((b) => ({ at: b.lastRunAt, kind: 'BATCH', level: 'WARN', title: `${b.name} ${b.lastStatus}`, detail: b.lastMessage })),
    ...db.audit.map((a) => ({ at: a.createdAt, kind: 'AUDIT', level: 'INFO', title: `${a.action} ${a.target}`, detail: a.detail })),
  ].sort((a, b) => (a.at < b.at ? 1 : -1))],
  ['GET', /^\/api\/ops\/deployments$/, () => db.deployments],
  ['GET', /^\/api\/ops\/batches$/, () => db.batches],
  ['GET', /^\/api\/ops\/app-versions$/, () => db.appVersions],
  ['PUT', /^\/api\/ops\/app-versions\/([^/]+)$/, (m, b) => {
    const v = db.appVersions.find((x) => x.platform === m[1])
    if (!v) throw new Conflict(`알 수 없는 플랫폼입니다: ${m[1]}`)
    if (role(String(b.userId)) !== 'OPS_LEAD') throw new Conflict('운영 팀장(OPS_LEAD)만 앱 버전 기준을 바꿀 수 있습니다.')
    const latest = String(b.latestVersion)
    const min = String(b.minSupportedVersion)
    const valid = (s: string) => /^\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(s)
    if (!valid(latest) || !valid(min)) throw new Conflict('버전은 3.8.0처럼 숫자 세 개로 입력하세요.')
    const cmp = (a: string, c: string) => {
      const x = a.split('.').map(Number)
      const y = c.split('.').map(Number)
      for (let i = 0; i < 3; i++) if (x[i] !== y[i]) return x[i]! - y[i]!
      return 0
    }
    if (cmp(min, latest) > 0) throw new Conflict('최소 지원 버전은 최신 버전보다 높을 수 없습니다.')
    audit(String(b.userId), 'APP_VERSION_UPDATED', v.platform, `최신 ${v.latestVersion} → ${latest}, 최소 ${v.minSupportedVersion} → ${min}`)
    Object.assign(v, { latestVersion: latest, minSupportedVersion: min, updatedAt: now() })
    return v
  }],
  ['GET', /^\/api\/ops\/audit$/, () => [...db.audit].reverse()],
]

const json = (status: number, body: unknown) =>
  new Response(body === null ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

export async function handle(method: string, path: string, body: Record<string, unknown>, headers: Headers): Promise<Response> {
  for (const [m, re, handler] of routes) {
    const match = m === method ? path.match(re) : null
    if (!match) continue
    try {
      const result = await handler(match, body, headers)
      if (result === undefined) return json(404, { message: '없습니다.' })
      return result === null ? new Response(null, { status: 204 }) : json(200, result)
    } catch (e) {
      if (e instanceof Conflict) return json(409, { message: e.message })
      throw e
    }
  }
  return json(404, { message: `데모에서 지원하지 않는 주소입니다: ${method} ${path}` })
}

/** window.fetch 를 바꿔서 /api 요청을 이 가짜 서버로 보낸다 */
export function installDemoApi() {
  const original = window.fetch.bind(window)
  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
    const path = url.startsWith('/') ? url : new URL(url).pathname
    if (!path.startsWith('/api/')) return original(input, init)
    const body = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {}
    return handle(init?.method ?? 'GET', path.split('?')[0]!, body, new Headers(init?.headers))
  }
}
