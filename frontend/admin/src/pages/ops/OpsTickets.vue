<script setup lang="ts">
import { ref } from 'vue'
import { postJson, useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label } from '../../format'
import { currentStaff, staffName } from '../../staffContext'
import type { Ticket } from '../../types'

const { data, error, loading, reload } = useJson<Ticket[]>(() => '/api/ops/tickets')
const form = ref({ inquiryId: 'INQ-0001', area: 'APP', severity: 'HIGH', title: '결제 후 결제대기로 표시됨', description: '' })
const message = ref<{ kind: 'ok' | 'error'; text: string }>()

async function create() {
  message.value = undefined
  try {
    const t = await postJson<Ticket>('/api/ops/tickets', {
      ...form.value,
      inquiryId: form.value.inquiryId || null,
      description: form.value.description || null,
      createdBy: currentStaff.value,
    })
    message.value = { kind: 'ok', text: `${t.ticketId} 티켓을 등록했습니다.` }
    await reload()
  } catch (e) {
    message.value = { kind: 'error', text: e instanceof Error ? e.message : String(e) }
  }
}
</script>

<template>
  <h1>SR 티켓</h1>
  <form class="panel form" @submit.prevent="create">
    <h2>새 티켓</h2>
    <label>문의번호 <input v-model="form.inquiryId" placeholder="없으면 비워 두기" /></label>
    <label>영역
      <select v-model="form.area"><option value="MALL">몰</option><option value="PARTNER">파트너</option><option value="CRM">CRM</option><option value="APP">앱</option></select>
    </label>
    <label>심각도
      <select v-model="form.severity"><option value="HIGH">높음</option><option value="MEDIUM">보통</option><option value="LOW">낮음</option></select>
    </label>
    <label class="wide">제목 <input v-model="form.title" /></label>
    <label class="wide">설명 <input v-model="form.description" /></label>
    <button type="submit">등록</button>
  </form>
  <p v-if="message" :class="message.kind === 'ok' ? 'ok' : 'error'" role="status">{{ message.text }}</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data && data.length" class="grid">
    <thead><tr><th>티켓</th><th>영역</th><th>심각도</th><th>제목</th><th>문의</th><th>등록자</th><th>상태</th><th>등록 일시</th></tr></thead>
    <tbody>
      <tr v-for="t in data" :key="t.ticketId">
        <td>{{ t.ticketId }}</td>
        <td>{{ label(t.area) }}</td>
        <td><span class="tag" :class="{ danger: t.severity === 'HIGH' }">{{ label(t.severity) }}</span></td>
        <td>{{ t.title }}</td>
        <td>{{ t.inquiryId ?? '-' }}</td>
        <td>{{ staffName(t.createdBy) }}</td>
        <td><span class="tag">{{ label(t.status) }}</span></td>
        <td>{{ dateTime(t.createdAt) }}</td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="data" class="muted">등록된 티켓이 없습니다.</p>
</template>
