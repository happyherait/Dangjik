<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label } from '../../format'
import type { BatchJob } from '../../types'

const { data, error, loading } = useJson<BatchJob[]>(() => '/api/ops/batches')
</script>

<template>
  <h1>배치 모니터링</h1>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>배치</th><th>이름</th><th>결과</th><th>메시지</th><th>마지막 실행</th></tr></thead>
    <tbody>
      <tr v-for="b in data" :key="b.jobId">
        <td>{{ b.jobId }}</td>
        <td>{{ b.name }}</td>
        <td><span class="tag" :class="{ warn: b.lastStatus !== 'SUCCESS' }">{{ label(b.lastStatus) }}</span></td>
        <td>{{ b.lastMessage }}</td>
        <td>{{ b.lastRunAt ? dateTime(b.lastRunAt) : '-' }}</td>
      </tr>
    </tbody>
  </table>
</template>
