<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { label, won } from '../../format'
import { currentPartnerId } from '../../partnerContext'
import type { PartnerProduct } from '../../types'

const { data, error, loading } = useJson<PartnerProduct[]>(() => `/api/partner/${currentPartnerId.value}/products`)
</script>

<template>
  <h1>상품 등록/수정</h1>
  <p class="muted">상품 등록과 수정 화면은 다음 단계에서 만듭니다. 지금은 목록만 봅니다.</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>상품번호</th><th>상품명</th><th>카테고리</th><th class="num">판매가</th><th>상태</th></tr></thead>
    <tbody>
      <tr v-for="p in data" :key="p.productId">
        <td>{{ p.productId }}</td>
        <td>{{ p.name }}</td>
        <td>{{ p.category }}</td>
        <td class="num">{{ won(p.price) }}</td>
        <td><span class="tag">{{ label(p.status) }}</span></td>
      </tr>
    </tbody>
  </table>
</template>
