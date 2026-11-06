<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label } from '../../format'
import { staffName } from '../../staffContext'
import type { RunView } from '../../types'

const { data, error, loading } = useJson<RunView[]>(() => '/api/ops/runbook-runs')
</script>

<template>
  <h1>런북 실행/승인</h1>
  <p class="muted">AI나 운영 담당이 제안한 런북입니다. 운영 팀장이 내용을 확인하고 승인해야 실행됩니다.</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data && data.length" class="grid">
    <thead><tr><th>실행번호</th><th>런북</th><th>대상</th><th>제안자</th><th>상태</th><th>제안 일시</th></tr></thead>
    <tbody>
      <tr v-for="r in data" :key="r.runId">
        <td><RouterLink :to="`/ops/runbooks/${r.runId}`" class="link">{{ r.runId }}</RouterLink></td>
        <td>결제 상태 재동기화</td>
        <td>{{ r.proposal.changes.map((c) => c.orderId).join(', ') }}</td>
        <td>{{ staffName(r.requestedBy) }}</td>
        <td><span class="tag" :class="{ warn: r.status === 'WAITING_APPROVAL' }">{{ label(r.status) }}</span></td>
        <td>{{ dateTime(r.createdAt) }}</td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="data" class="muted">제안된 런북이 없습니다. AI 당직에게 장애를 물어보세요.</p>
</template>
