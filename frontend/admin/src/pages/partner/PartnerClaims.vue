<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label } from '../../format'
import { currentPartnerId } from '../../partnerContext'
import type { PartnerClaim } from '../../types'

const { data, error, loading } = useJson<PartnerClaim[]>(() => `/api/partner/${currentPartnerId.value}/claims`)
</script>

<template>
  <h1>클레임</h1>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data && data.length" class="grid">
    <thead><tr><th>클레임번호</th><th>주문번호</th><th>유형</th><th>사유</th><th>상태</th><th>접수일시</th></tr></thead>
    <tbody>
      <tr v-for="c in data" :key="c.claimId">
        <td>{{ c.claimId }}</td>
        <td>{{ c.orderId }}</td>
        <td>{{ label(c.claimType) }}</td>
        <td>{{ c.reason }}</td>
        <td><span class="tag">{{ label(c.status) }}</span></td>
        <td>{{ dateTime(c.requestedAt) }}</td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="data" class="muted">처리할 클레임이 없습니다.</p>
</template>
