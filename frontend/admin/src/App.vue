<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { areas } from './menu'
import PartnerPicker from './components/PartnerPicker.vue'
import StaffPicker from './components/StaffPicker.vue'

const route = useRoute()
const currentArea = computed(() => areas.find((a) => route.path.startsWith(`/${a.key}/`)))
const isDemo = import.meta.env.VITE_DEMO === 'true'
</script>

<template>
  <div class="shell">
    <div v-if="isDemo" class="demo-banner" role="note">
      공개 데모입니다. 서버 없이 브라우저 안에서만 동작하고, AI는 정해진 순서로 조회하는 데모 모델입니다. 새로고침하면 처음 상태로 돌아갑니다.
    </div>
    <header class="top">
      <RouterLink to="/" class="brand">하루마켓 관리자 <small>당직</small></RouterLink>
      <nav class="areas">
        <RouterLink
          v-for="area in areas"
          :key="area.key"
          :to="area.items[0].path"
          :class="{ active: currentArea?.key === area.key }"
        >
          {{ area.title }}
        </RouterLink>
      </nav>
    </header>
    <div class="body">
      <aside v-if="currentArea" class="side" :aria-label="`${currentArea.title} 메뉴`">
        <p class="audience">{{ currentArea.audience }}</p>
        <PartnerPicker v-if="currentArea.key === 'partner'" />
        <StaffPicker v-if="currentArea.key === 'ops'" />
        <RouterLink v-for="item in currentArea.items" :key="item.path" :to="item.path">
          {{ item.title }}
          <small v-if="item.status === 'planned'" class="soon">준비 중</small>
        </RouterLink>
      </aside>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
