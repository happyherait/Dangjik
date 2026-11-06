<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { label, won } from '../../format'
import { currentPartnerId } from '../../partnerContext'
import type { PartnerDashboard } from '../../types'

const { data, error, loading } = useJson<PartnerDashboard>(() => `/api/partner/${currentPartnerId.value}/dashboard`)
</script>

<template>
  <h1>대시보드</h1>
  <LoadState :loading="loading" :error="error" />
  <template v-if="data">
    <section class="stats">
      <RouterLink to="/partner/products" class="stat"><b>{{ data.onSale }}</b><span>판매중 상품</span></RouterLink>
      <RouterLink to="/partner/stock" class="stat"><b>{{ data.soldOut }}</b><span>품절</span></RouterLink>
      <RouterLink to="/partner/orders" class="stat accent"><b>{{ data.readyToShip }}</b><span>출고 대기</span></RouterLink>
      <RouterLink to="/partner/orders" class="stat"><b>{{ data.waitingPayment }}</b><span>결제 확인 대기</span></RouterLink>
      <RouterLink to="/partner/claims" class="stat"><b>{{ data.openClaims }}</b><span>처리할 클레임</span></RouterLink>
    </section>
    <section v-if="data.latestSettlement" class="panel">
      <h2>최근 정산 ({{ data.latestSettlement.period }})</h2>
      <p>
        정산 금액 <strong>{{ won(data.latestSettlement.netAmount) }}</strong>
        · {{ label(data.latestSettlement.status) }}
      </p>
    </section>
  </template>
</template>
