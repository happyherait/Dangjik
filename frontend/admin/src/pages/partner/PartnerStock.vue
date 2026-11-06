<script setup lang="ts">
import { computed } from 'vue'
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { LOW_STOCK, stockLevel } from '../../format'
import { currentPartnerId } from '../../partnerContext'
import type { PartnerProduct } from '../../types'

const { data, error, loading } = useJson<PartnerProduct[]>(() => `/api/partner/${currentPartnerId.value}/products`)
const sorted = computed(() => [...(data.value ?? [])].sort((a, b) => a.stock - b.stock))
</script>

<template>
  <h1>재고</h1>
  <p class="muted">재고가 {{ LOW_STOCK }}개 미만이면 부족으로 표시합니다. 적은 순서로 보여줍니다.</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>상품번호</th><th>상품명</th><th class="num">재고</th><th>상태</th></tr></thead>
    <tbody>
      <tr v-for="p in sorted" :key="p.productId">
        <td>{{ p.productId }}</td>
        <td>{{ p.name }}</td>
        <td class="num">{{ p.stock }}</td>
        <td>
          <span v-if="stockLevel(p.stock) === 'out'" class="tag danger">품절</span>
          <span v-else-if="stockLevel(p.stock) === 'low'" class="tag warn">부족</span>
          <span v-else class="tag">충분</span>
        </td>
      </tr>
    </tbody>
  </table>
</template>
