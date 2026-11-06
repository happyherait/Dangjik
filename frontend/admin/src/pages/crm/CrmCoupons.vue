<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { label } from '../../format'
import type { Coupon } from '../../types'

const { data, error, loading } = useJson<Coupon[]>(() => '/api/crm/coupons')
</script>

<template>
  <h1>쿠폰/프로모션</h1>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>쿠폰</th><th>이름</th><th class="num">할인율</th><th>사용 기한</th><th>상태</th></tr></thead>
    <tbody>
      <tr v-for="c in data" :key="c.couponId">
        <td>{{ c.couponId }}</td>
        <td>{{ c.name }}</td>
        <td class="num">{{ c.discountRate }}%</td>
        <td>{{ c.validTo }}</td>
        <td><span class="tag">{{ label(c.status) }}</span></td>
      </tr>
    </tbody>
  </table>
</template>
