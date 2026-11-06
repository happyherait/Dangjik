<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime } from '../../format'
import type { TimelineEvent } from '../../types'

const { data, error, loading } = useJson<TimelineEvent[]>(() => '/api/ops/timeline')
const KIND: Record<string, string> = { DEPLOY: '배포', WEBHOOK: '웹훅', BATCH: '배치', AUDIT: '조치' }
</script>

<template>
  <h1>장애 타임라인</h1>
  <p class="muted">배포, 웹훅 실패, 배치 경고, 운영 조치를 최신순으로 모았습니다.</p>
  <LoadState :loading="loading" :error="error" />
  <ol v-if="data" class="timeline">
    <li v-for="(e, i) in data" :key="i" :class="e.level.toLowerCase()">
      <span class="when">{{ dateTime(e.at) }}</span>
      <span class="tag">{{ KIND[e.kind] ?? e.kind }}</span>
      <strong>{{ e.title }}</strong>
      <span v-if="e.detail" class="muted"> · {{ e.detail }}</span>
    </li>
  </ol>
</template>
