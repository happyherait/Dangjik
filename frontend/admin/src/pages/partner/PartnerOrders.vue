<script setup lang="ts">
import { ref } from 'vue'
import { post, useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label, maskName, won } from '../../format'
import { currentPartnerId, currentPartnerUser } from '../../partnerContext'
import type { PartnerOrderLine } from '../../types'

const { data, error, loading, reload } = useJson<PartnerOrderLine[]>(
  () => `/api/partner/${currentPartnerId.value}/orders`,
)
const message = ref<{ kind: 'ok' | 'error'; text: string }>()

async function ship(line: PartnerOrderLine) {
  message.value = undefined
  try {
    await post(
      `/api/partner/${currentPartnerId.value}/orders/${line.orderId}/lines/${line.lineNo}/ship`,
      currentPartnerUser(),
    )
    message.value = { kind: 'ok', text: `${line.orderId} ${line.productName} 출고 처리했습니다.` }
    await reload()
  } catch (e) {
    message.value = { kind: 'error', text: e instanceof Error ? e.message : String(e) }
  }
}
</script>

<template>
  <h1>주문/출고</h1>
  <p class="muted">고객 이름은 개인정보 보호를 위해 가려서 보여줍니다. 결제가 확인된 주문만 출고할 수 있습니다.</p>
  <p v-if="message" :class="message.kind === 'ok' ? 'ok' : 'error'" role="status">{{ message.text }}</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead>
      <tr><th>주문번호</th><th>주문일시</th><th>고객</th><th>상품</th><th class="num">수량</th><th class="num">금액</th><th>주문</th><th>출고</th><th></th></tr>
    </thead>
    <tbody>
      <tr v-for="l in data" :key="`${l.orderId}-${l.lineNo}`">
        <td>{{ l.orderId }}</td>
        <td>{{ dateTime(l.orderedAt) }}</td>
        <td>{{ maskName(l.memberName) }}</td>
        <td>{{ l.productName }}</td>
        <td class="num">{{ l.qty }}</td>
        <td class="num">{{ won(l.unitPrice * l.qty) }}</td>
        <td><span class="tag" :class="{ warn: l.orderStatus === 'PAYMENT_PENDING' }">{{ label(l.orderStatus) }}</span></td>
        <td>{{ label(l.shipmentStatus) }}</td>
        <td>
          <button v-if="l.shipmentStatus === 'READY'" type="button" @click="ship(l)">출고</button>
          <span v-else-if="l.shipmentStatus === 'WAITING_PAYMENT'" class="muted small">결제 확인 후 출고</span>
        </td>
      </tr>
    </tbody>
  </table>
  <p v-if="data && data.length === 0" class="muted">주문이 없습니다.</p>
</template>
