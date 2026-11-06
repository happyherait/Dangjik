<template>
  <header class="header">
    <div class="wrap">
      <NuxtLink to="/" class="logo">하루마켓</NuxtLink>
      <nav class="nav">
        <NuxtLink to="/products">전체 상품</NuxtLink>
        <NuxtLink to="/orders">주문 조회</NuxtLink>
      </nav>
    </div>
  </header>
  <main class="wrap">
    <NuxtPage />
  </main>
</template>
