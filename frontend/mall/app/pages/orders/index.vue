<script setup lang="ts">
const orderId = ref('')
const invalid = ref(false)

function lookup() {
  const value = orderId.value.trim().toUpperCase()
  invalid.value = !ORDER_ID_PATTERN.test(value)
  if (!invalid.value) {
    navigateTo(`/orders/${value}`)
  }
}

useHead({ title: '주문 조회 · 하루마켓' })
</script>

<template>
  <h1>주문 조회</h1>
  <form class="lookup" @submit.prevent="lookup">
    <input v-model="orderId" placeholder="주문번호 예: ORD-20261105-0007" aria-label="주문번호" />
    <button type="submit">조회</button>
  </form>
  <p v-if="invalid" class="muted">주문번호 형식을 확인해 주세요.</p>
</template>
