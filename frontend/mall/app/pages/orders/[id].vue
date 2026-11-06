<script setup lang="ts">
import type { Order } from '~/utils/mall'

const route = useRoute()
const id = String(route.params.id)
const { data: order, error } = await useFetch<Order>(`/api/orders/${encodeURIComponent(id)}`)

useHead({ title: `주문 ${id} · 하루마켓` })
</script>

<template>
  <template v-if="order">
    <h1>주문 {{ order.orderId }}</h1>
    <p>
      <span class="badge" :class="{ warn: order.status === 'PAYMENT_PENDING' }">
        {{ orderStatusLabel(order.status) }}
      </span>
      <span class="muted"> · {{ order.channel === 'APP' ? '앱' : '웹' }} 주문 · {{ order.orderedAt.replace('T', ' ') }}</span>
    </p>
    <div v-if="order.status === 'PAYMENT_PENDING'" class="notice">
      결제 확인이 늦어지고 있습니다. 카드 승인 문자를 받으셨다면 고객센터로 문의해 주세요.
    </div>
    <div class="box">
      <table>
        <thead>
          <tr><th>상품</th><th>수량</th><th>금액</th><th>배송</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in order.items" :key="item.lineNo">
            <td><NuxtLink :to="`/products/${item.productId}`">{{ item.productName }}</NuxtLink></td>
            <td>{{ item.qty }}</td>
            <td>{{ formatPrice(item.unitPrice * item.qty) }}</td>
            <td>{{ shipmentStatusLabel(item.shipmentStatus) }}</td>
          </tr>
        </tbody>
      </table>
      <p style="text-align: right"><strong>합계 {{ formatPrice(order.totalAmount) }}</strong></p>
    </div>
  </template>
  <div v-else-if="error">
    <h1>주문을 찾을 수 없습니다</h1>
    <NuxtLink to="/orders" class="muted">다시 조회하기</NuxtLink>
  </div>
</template>
