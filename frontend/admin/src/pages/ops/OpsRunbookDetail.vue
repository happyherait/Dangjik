<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { postJson, useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label, won } from '../../format'
import { currentStaff, staffName } from '../../staffContext'
import type { RunView } from '../../types'

const route = useRoute()
const runId = String(route.params.runId)
const { data, error, loading, reload } = useJson<RunView>(() => `/api/ops/runbook-runs/${runId}`)
const message = ref<{ kind: 'ok' | 'error'; text: string }>()
const rejectReason = ref('')

async function decide(kind: 'approve' | 'reject') {
  if (!data.value) return
  message.value = undefined
  try {
    const body =
      kind === 'approve'
        ? { approverId: currentStaff.value, planHash: data.value.planHash }
        : { approverId: currentStaff.value, reason: rejectReason.value || '반려' }
    await postJson<RunView>(`/api/ops/runbook-runs/${runId}/${kind}`, body)
    message.value = { kind: 'ok', text: kind === 'approve' ? '승인하고 실행했습니다.' : '반려했습니다.' }
    await reload()
  } catch (e) {
    message.value = { kind: 'error', text: e instanceof Error ? e.message : String(e) }
  }
}
</script>

<template>
  <p><RouterLink to="/ops/runbooks" class="link">← 런북 목록</RouterLink></p>
  <LoadState :loading="loading" :error="error" />
  <template v-if="data">
    <h1>{{ data.runId }} <span class="tag" :class="{ warn: data.status === 'WAITING_APPROVAL' }">{{ label(data.status) }}</span></h1>
    <p class="muted">
      결제 상태 재동기화 · 제안 {{ staffName(data.requestedBy) }} · {{ dateTime(data.createdAt) }}
      <template v-if="data.ticketId"> · 티켓 {{ data.ticketId }}</template>
    </p>
    <section class="panel">
      <h2>제안 사유</h2>
      <p>{{ data.proposal.reason }}</p>
    </section>
    <section class="panel">
      <h2>실행하면 바뀌는 것</h2>
      <table class="grid">
        <thead><tr><th>주문번호</th><th>변경</th><th>PG 거래</th><th class="num">금액</th></tr></thead>
        <tbody>
          <tr v-for="c in data.proposal.changes" :key="c.orderId">
            <td>{{ c.orderId }}</td>
            <td>{{ label(c.fromStatus) }} → <strong>{{ label(c.toStatus) }}</strong></td>
            <td>{{ c.pgTxId }}</td>
            <td class="num">{{ won(c.amount) }}</td>
          </tr>
        </tbody>
      </table>
      <template v-if="data.proposal.skipped.length">
        <h2 style="margin-top: 16px">제외된 주문</h2>
        <ul>
          <li v-for="s in data.proposal.skipped" :key="s.orderId">{{ s.orderId }}: {{ s.reason }}</li>
        </ul>
      </template>
      <p class="muted small">계획 해시 {{ data.planHash.slice(0, 16) }}… 승인은 이 계획에만 적용됩니다.</p>
    </section>
    <p v-if="data.stale" class="error">제안 이후 주문이나 PG 데이터가 바뀌었습니다. 이 계획으로는 실행할 수 없으니 다시 제안하세요.</p>
    <p v-if="message" :class="message.kind === 'ok' ? 'ok' : 'error'" role="status">{{ message.text }}</p>
    <section v-if="data.status === 'WAITING_APPROVAL'" class="panel decide">
      <p>
        <strong>{{ staffName(currentStaff) }}</strong>(으)로 결정합니다. 운영 팀장만, 그리고 제안자가 아닌 사람만 승인할 수 있습니다.
        직원은 왼쪽 메뉴에서 바꿀 수 있습니다.
      </p>
      <div class="row">
        <button type="button" @click="decide('approve')">승인하고 실행</button>
        <input v-model="rejectReason" placeholder="반려 사유" aria-label="반려 사유" />
        <button type="button" class="ghost" @click="decide('reject')">반려</button>
      </div>
    </section>
  </template>
</template>
