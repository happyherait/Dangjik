<script setup lang="ts">
import { useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime } from '../../format'
import type { AppVersion } from '../../types'

const { data, error, loading } = useJson<AppVersion[]>(() => '/api/ops/app-versions')
</script>

<template>
  <h1>앱 버전 관리</h1>
  <p class="muted">최소 지원 버전보다 낮은 앱은 강제 업데이트 안내를 받습니다. 수정 기능은 다음 단계에서 만듭니다.</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>플랫폼</th><th>최신 버전</th><th>최소 지원 버전</th><th>변경 일시</th></tr></thead>
    <tbody>
      <tr v-for="v in data" :key="v.platform">
        <td>{{ v.platform === 'IOS' ? 'iOS' : 'Android' }}</td>
        <td>{{ v.latestVersion }}</td>
        <td>{{ v.minSupportedVersion }}</td>
        <td>{{ dateTime(v.updatedAt) }}</td>
      </tr>
    </tbody>
  </table>
</template>
