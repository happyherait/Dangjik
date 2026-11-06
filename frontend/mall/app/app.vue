<script setup lang="ts">
const { app } = useAppBridge()
</script>

<template>
  <header class="header">
    <div class="wrap">
      <NuxtLink to="/" class="logo">하루마켓</NuxtLink>
      <nav class="nav">
        <NuxtLink to="/products">전체 상품</NuxtLink>
        <NuxtLink to="/orders">주문 조회</NuxtLink>
      </nav>
      <span v-if="app" class="badge app-badge">앱 {{ app.platform === 'IOS' ? 'iOS' : 'Android' }} {{ app.version }}</span>
    </div>
  </header>
  <main class="wrap">
    <NuxtPage />
  </main>
</template>
