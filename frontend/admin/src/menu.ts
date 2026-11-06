// 관리자 화면의 메뉴 구성. 몰(고객 화면)은 frontend/mall 의 Nuxt 앱이 따로 맡는다.

export type AreaKey = 'partner' | 'crm' | 'ops'

export interface MenuItem {
  path: string
  title: string
  /** 'ready': 구현됨, 'planned': 아직 자리만 있음 */
  status: 'ready' | 'planned'
}

export interface Area {
  key: AreaKey
  title: string
  audience: string
  items: MenuItem[]
}

export const areas: Area[] = [
  {
    key: 'partner',
    title: '파트너 포털',
    audience: '입점사',
    items: [
      { path: '/partner/dashboard', title: '대시보드', status: 'ready' },
      { path: '/partner/products', title: '상품 등록/수정', status: 'ready' },
      { path: '/partner/stock', title: '재고', status: 'ready' },
      { path: '/partner/orders', title: '주문/출고', status: 'ready' },
      { path: '/partner/claims', title: '클레임', status: 'ready' },
      { path: '/partner/settlements', title: '정산 내역', status: 'ready' },
      { path: '/partner/notices', title: '공지/문의', status: 'planned' },
    ],
  },
  {
    key: 'crm',
    title: 'CRM',
    audience: '상담, 마케팅',
    items: [
      { path: '/crm/members', title: '회원 360', status: 'ready' },
      { path: '/crm/inquiries', title: '상담 이력', status: 'ready' },
      { path: '/crm/coupons', title: '쿠폰/프로모션', status: 'ready' },
      { path: '/crm/segments', title: '회원 세그먼트', status: 'ready' },
      { path: '/crm/campaigns', title: '캠페인 발송', status: 'planned' },
    ],
  },
  {
    key: 'ops',
    title: '운영 콘솔',
    audience: '유지보수 담당',
    items: [
      { path: '/ops/chat', title: 'AI 당직', status: 'planned' },
      { path: '/ops/tickets', title: 'SR 티켓', status: 'planned' },
      { path: '/ops/incidents', title: '장애 타임라인', status: 'planned' },
      { path: '/ops/runbooks', title: '런북 실행/승인', status: 'planned' },
      { path: '/ops/batches', title: '배치 모니터링', status: 'planned' },
      { path: '/ops/deployments', title: '배포 이력', status: 'planned' },
      { path: '/ops/app-versions', title: '앱 버전 관리', status: 'planned' },
      { path: '/ops/audit', title: '감사 로그', status: 'planned' },
    ],
  },
]

export function findMenuItem(path: string): MenuItem | undefined {
  return areas.flatMap((a) => a.items).find((i) => i.path === path)
}
