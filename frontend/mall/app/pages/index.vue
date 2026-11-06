<script setup lang="ts">
import type { Product } from '~/utils/mall'

const { data: products } = await useFetch<Product[]>('/api/products', { default: () => [] })
const onSale = computed(() => products.value.filter((p) => !isSoldOut(p)).slice(0, 4))
</script>

<template>
  <section class="hero">
    <h1>오늘 딴 것, 오늘 구운 것</h1>
    <p>입점 농가와 공방의 상품을 하루 만에 보내 드립니다. (가상 쇼핑몰)</p>
  </section>
  <h2>지금 인기 있는 상품</h2>
  <div class="grid">
    <ProductCard v-for="p in onSale" :key="p.productId" :product="p" />
  </div>
  <p><NuxtLink to="/products" class="muted">전체 상품 보기 →</NuxtLink></p>
</template>
