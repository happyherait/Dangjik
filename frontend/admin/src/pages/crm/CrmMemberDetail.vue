<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime, label, won } from '../../format'
import type { Member360 } from '../../types'

const route = useRoute()
const { data, error, loading } = useJson<Member360>(() => `/api/crm/members/${String(route.params.memberId)}`)
</script>

<template>
  <p><RouterLink to="/crm/members" class="link">← 회원 목록</RouterLink></p>
  <LoadState :loading="loading" :error="error" />
  <template v-if="data">
    <h1>{{ data.member.name }} <span class="tag">{{ data.member.grade }}</span></h1>
    <p class="muted">{{ data.member.memberId }} · {{ data.member.email }} · 가입 {{ data.member.joinedAt }}</p>
    <section class="stats">
      <div class="stat"><b>{{ data.orders.length }}</b><span>주문</span></div>
      <div class="stat"><b>{{ won(data.totalSpent) }}</b><span>결제 확인된 구매액</span></div>
      <div class="stat" :class="{ accent: data.inquiries.some((q) => q.status === 'OPEN') }">
        <b>{{ data.inquiries.filter((q) => q.status === 'OPEN').length }}</b><span>처리중 문의</span>
      </div>
    </section>
    <section class="panel">
      <h2>주문</h2>
      <table class="grid">
        <thead><tr><th>주문번호</th><th>채널</th><th>상태</th><th class="num">금액</th><th>주문일시</th></tr></thead>
        <tbody>
          <tr v-for="o in data.orders" :key="o.orderId">
            <td>{{ o.orderId }}</td>
            <td>{{ label(o.channel) }}</td>
            <td><span class="tag" :class="{ warn: o.status === 'PAYMENT_PENDING' }">{{ label(o.status) }}</span></td>
            <td class="num">{{ won(o.totalAmount) }}</td>
            <td>{{ dateTime(o.orderedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </section>
    <section class="panel">
      <h2>상담 이력</h2>
      <article v-for="q in data.inquiries" :key="q.inquiryId" class="inquiry">
        <header><strong>{{ q.title }}</strong> <span class="tag">{{ label(q.status) }}</span></header>
        <p>{{ q.body }}</p>
        <p class="muted small">{{ q.inquiryId }} · {{ label(q.channel) }} · {{ dateTime(q.createdAt) }}<template v-if="q.orderId"> · 주문 {{ q.orderId }}</template></p>
      </article>
      <p v-if="data.inquiries.length === 0" class="muted">상담 이력이 없습니다.</p>
    </section>
  </template>
</template>
