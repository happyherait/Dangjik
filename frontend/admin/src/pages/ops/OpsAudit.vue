<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime } from '../../format'
import type { AuditEvent } from '../../types'

const { data, error, loading } = useJson<AuditEvent[]>(() => '/api/ops/audit')
</script>

<template>
  <h1>감사 로그</h1>
  <p class="muted">상태를 바꾸거나 AI에게 묻는 모든 행동이 남습니다. 최근 200건.</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data && data.length" class="grid">
    <thead><tr><th>#</th><th>일시</th><th>누가</th><th>행동</th><th>대상</th><th>내용</th></tr></thead>
    <tbody>
      <tr v-for="a in data" :key="a.eventId">
        <td>{{ a.eventId }}</td>
        <td>{{ dateTime(a.createdAt) }}</td>
        <td>{{ a.actorId }}</td>
        <td><code>{{ a.action }}</code></td>
        <td>{{ a.target }}</td>
        <td class="wrap">{{ a.detail }}</td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="data" class="muted">아직 기록이 없습니다.</p>
</template>
