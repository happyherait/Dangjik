<script setup lang="ts">
import { computed } from 'vue'
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import type { Segment } from '../../types'

const { data, error, loading } = useJson<Segment[]>(() => '/api/crm/segments')
const total = computed(() => (data.value ?? []).reduce((sum, s) => sum + s.members, 0))
</script>

<template>
  <h1>회원 세그먼트</h1>
  <p class="muted">등급별 회원 수입니다. 캠페인 발송 대상을 고를 때 씁니다.</p>
  <LoadState :loading="loading" :error="error" />
  <section v-if="data" class="panel">
    <div v-for="s in data" :key="s.grade" class="bar-row">
      <span class="bar-label">{{ s.grade }}</span>
      <span class="bar" :style="{ width: `${total ? (s.members / total) * 100 : 0}%` }" />
      <span class="bar-value">{{ s.members }}명</span>
    </div>
  </section>
</template>
