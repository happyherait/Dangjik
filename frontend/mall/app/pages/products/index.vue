<script setup lang="ts">
import type { Product } from '~/utils/mall'

const route = useRoute()
const { data: products } = await useFetch<Product[]>('/api/products', { default: () => [] })

const selected = computed(() => (typeof route.query.category === 'string' ? route.query.category : undefined))
const categories = computed(() => categoriesOf(products.value))
const visible = computed(() => filterByCategory(products.value, selected.value))

useHead({ title: '전체 상품 · 하루마켓' })
</script>

<template>
  <h1>전체 상품</h1>
  <nav class="chips" aria-label="카테고리">
    <NuxtLink to="/products" :class="{ on: !selected }">전체</NuxtLink>
    <NuxtLink
      v-for="c in categories"
      :key="c"
      :to="{ path: '/products', query: { category: c } }"
      :class="{ on: selected === c }"
    >
      {{ c }}
    </NuxtLink>
  </nav>
  <div class="grid">
    <ProductCard v-for="p in visible" :key="p.productId" :product="p" />
  </div>
  <p v-if="visible.length === 0" class="muted">상품이 없습니다.</p>
</template>
