<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label } from '../../format'
import type { Inquiry } from '../../types'

const { data, error, loading } = useJson<Inquiry[]>(() => '/api/crm/inquiries')
</script>

<template>
  <h1>상담 이력</h1>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>문의번호</th><th>회원</th><th>제목</th><th>주문번호</th><th>채널</th><th>상태</th><th>접수일시</th></tr></thead>
    <tbody>
      <tr v-for="q in data" :key="q.inquiryId">
        <td>{{ q.inquiryId }}</td>
        <td><RouterLink :to="`/crm/members/${q.memberId}`" class="link">{{ q.memberName }}</RouterLink></td>
        <td>{{ q.title }}</td>
        <td>{{ q.orderId ?? '-' }}</td>
        <td>{{ label(q.channel) }}</td>
        <td><span class="tag" :class="{ warn: q.status === 'OPEN' }">{{ label(q.status) }}</span></td>
        <td>{{ dateTime(q.createdAt) }}</td>
      </tr>
    </tbody>
  </table>
</template>
