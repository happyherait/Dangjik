<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime } from '../../format'
import { staffName } from '../../staffContext'
import type { Deployment } from '../../types'

const { data, error, loading } = useJson<Deployment[]>(() => '/api/ops/deployments')
</script>

<template>
  <h1>배포 이력</h1>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>배포</th><th>대상</th><th>버전</th><th>내용</th><th>배포자</th><th>일시</th></tr></thead>
    <tbody>
      <tr v-for="d in data" :key="d.deployId">
        <td>{{ d.deployId }}</td>
        <td>{{ d.area }}</td>
        <td>{{ d.version }}</td>
        <td>{{ d.note }}</td>
        <td>{{ staffName(d.deployedBy) }}</td>
        <td>{{ dateTime(d.deployedAt) }}</td>
      </tr>
    </tbody>
  </table>
</template>
