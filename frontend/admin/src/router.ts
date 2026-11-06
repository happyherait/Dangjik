import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { areas } from './menu'
import HomePage from './pages/HomePage.vue'
import PlannedPage from './pages/PlannedPage.vue'

export const routes: RouteRecordRaw[] = [
  { path: '/', component: HomePage },
  ...areas.flatMap((area) =>
    area.items.map((item) => ({
      path: item.path,
      component: PlannedPage,
      meta: { area: area.key, title: item.title },
    })),
  ),
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

export function createAppRouter() {
  // GitHub Pages 공개 데모에서도 새로고침이 되도록 해시 라우팅을 쓴다
  return createRouter({ history: createWebHashHistory(), routes })
}
