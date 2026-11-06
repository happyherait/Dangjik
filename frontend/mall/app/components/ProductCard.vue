<script setup lang="ts">
import type { Product } from '~/utils/mall'

defineProps<{ product: Product }>()
</script>

<template>
  <NuxtLink :to="`/products/${product.productId}`" class="card" :class="{ soldout: isSoldOut(product) }">
    <div class="thumb" aria-hidden="true">{{ categoryIcon(product.category) }}</div>
    <div class="cat">{{ product.category }}</div>
    <p class="name">{{ product.name }}</p>
    <span class="price">{{ formatPrice(product.price) }}</span>
    <span v-if="isSoldOut(product)" class="badge"> 품절</span>
  </NuxtLink>
</template>
