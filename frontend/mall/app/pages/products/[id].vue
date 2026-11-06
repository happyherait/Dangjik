<script setup lang="ts">
import type { Product } from '~/utils/mall'

const route = useRoute()
const id = String(route.params.id)
const { data: product, error } = await useFetch<Product>(`/api/products/${encodeURIComponent(id)}`)

useHead({ title: () => (product.value ? `${product.value.name} · 하루마켓` : '상품 · 하루마켓') })
</script>

<template>
  <div v-if="product" class="detail">
    <div class="thumb" aria-hidden="true">{{ categoryIcon(product.category) }}</div>
    <div>
      <div class="cat">{{ product.category }} · 입점사 {{ product.partnerId }}</div>
      <h1>{{ product.name }}</h1>
      <p class="price" style="font-size: 24px">{{ formatPrice(product.price) }}</p>
      <!-- 입점사가 쓴 설명은 그대로 글자로만 보여준다 -->
      <p>{{ product.description }}</p>
      <p v-if="isSoldOut(product)" class="badge">품절</p>
      <p v-else class="muted">재고 {{ product.stock }}개 · 장바구니와 결제는 다음 단계에서 만듭니다.</p>
    </div>
  </div>
  <div v-else-if="error">
    <h1>상품을 찾을 수 없습니다</h1>
    <NuxtLink to="/products" class="muted">전체 상품으로 돌아가기</NuxtLink>
  </div>
</template>
