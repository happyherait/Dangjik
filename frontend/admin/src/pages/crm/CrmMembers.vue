<script setup lang="ts">
import { computed, ref } from 'vue'
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import type { Member } from '../../types'

const { data, error, loading } = useJson<Member[]>(() => '/api/crm/members')
const keyword = ref('')
const filtered = computed(() => {
  const k = keyword.value.trim()
  return (data.value ?? []).filter((m) => !k || m.name.includes(k) || m.memberId.includes(k) || m.email.includes(k))
})
</script>

<template>
  <h1>회원 360</h1>
  <input v-model="keyword" class="search" placeholder="이름, 회원번호, 이메일로 찾기" aria-label="회원 검색" />
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>회원번호</th><th>이름</th><th>이메일</th><th>등급</th><th>가입일</th></tr></thead>
    <tbody>
      <tr v-for="m in filtered" :key="m.memberId">
        <td><RouterLink :to="`/crm/members/${m.memberId}`" class="link">{{ m.memberId }}</RouterLink></td>
        <td>{{ m.name }}</td>
        <td>{{ m.email }}</td>
        <td><span class="tag">{{ m.grade }}</span></td>
        <td>{{ m.joinedAt }}</td>
      </tr>
    </tbody>
  </table>
</template>
