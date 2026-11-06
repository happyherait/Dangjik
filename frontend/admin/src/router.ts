import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import type { Component } from 'vue'
import { areas } from './menu'
import HomePage from './pages/HomePage.vue'
import PlannedPage from './pages/PlannedPage.vue'
import PartnerDashboard from './pages/partner/PartnerDashboard.vue'
import PartnerProducts from './pages/partner/PartnerProducts.vue'
import PartnerStock from './pages/partner/PartnerStock.vue'
import PartnerOrders from './pages/partner/PartnerOrders.vue'
import PartnerClaims from './pages/partner/PartnerClaims.vue'
import PartnerSettlements from './pages/partner/PartnerSettlements.vue'
import CrmMembers from './pages/crm/CrmMembers.vue'
import CrmMemberDetail from './pages/crm/CrmMemberDetail.vue'
import CrmInquiries from './pages/crm/CrmInquiries.vue'
import CrmCoupons from './pages/crm/CrmCoupons.vue'
import CrmSegments from './pages/crm/CrmSegments.vue'

/** 구현된 메뉴의 화면. 여기에 없는 메뉴는 '준비 중' 화면을 보여준다. */
export const pages: Record<string, Component> = {
  '/partner/dashboard': PartnerDashboard,
  '/partner/products': PartnerProducts,
  '/partner/stock': PartnerStock,
  '/partner/orders': PartnerOrders,
  '/partner/claims': PartnerClaims,
  '/partner/settlements': PartnerSettlements,
  '/crm/members': CrmMembers,
  '/crm/inquiries': CrmInquiries,
  '/crm/coupons': CrmCoupons,
  '/crm/segments': CrmSegments,
}

export const routes: RouteRecordRaw[] = [
  { path: '/', component: HomePage },
  ...areas.flatMap((area) =>
    area.items.map((item) => ({
      path: item.path,
      component: pages[item.path] ?? PlannedPage,
      meta: { area: area.key, title: item.title },
    })),
  ),
  { path: '/crm/members/:memberId', component: CrmMemberDetail, meta: { area: 'crm', title: '회원 상세' } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

export function createAppRouter() {
  // GitHub Pages 공개 데모에서도 새로고침이 되도록 해시 라우팅을 쓴다
  return createRouter({ history: createWebHashHistory(), routes })
}
