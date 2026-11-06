<script setup lang="ts">
import { areas } from '../menu'
</script>

<template>
  <h1>하루마켓 관리자</h1>
  <p class="lead">
    유통 커머스 사이트의 파트너 포털, CRM, 운영 콘솔입니다. 운영 콘솔의 AI 당직은 장애 원인을 진단하고 조치안을
    제안하지만, 조치 실행은 런북과 사람의 승인을 거쳐서만 이뤄집니다.
  </p>
  <section class="cards">
    <article v-for="area in areas" :key="area.key" class="card">
      <h2>{{ area.title }}</h2>
      <p>{{ area.audience }} · 메뉴 {{ area.items.length }}개</p>
      <RouterLink :to="area.items[0].path">열기</RouterLink>
    </article>
  </section>
</template>
