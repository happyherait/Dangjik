<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { label, won } from '../../format'
import { currentPartnerId } from '../../partnerContext'
import type { Settlement } from '../../types'

const { data, error, loading } = useJson<Settlement[]>(() => `/api/partner/${currentPartnerId.value}/settlements`)
</script>

<template>
  <h1>정산 내역</h1>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data && data.length" class="grid">
    <thead><tr><th>정산월</th><th class="num">판매액</th><th class="num">수수료</th><th class="num">정산액</th><th>상태</th></tr></thead>
    <tbody>
      <tr v-for="s in data" :key="s.settlementId">
        <td>{{ s.period }}</td>
        <td class="num">{{ won(s.grossAmount) }}</td>
        <td class="num">{{ won(s.commission) }}</td>
        <td class="num"><strong>{{ won(s.netAmount) }}</strong></td>
        <td><span class="tag">{{ label(s.status) }}</span></td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="data" class="muted">정산 내역이 없습니다.</p>
</template>
