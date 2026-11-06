<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { putJson, useJson } from '../../api'
import LoadState from '../../components/LoadState.vue'
import { dateTime } from '../../format'
import { currentStaff, staffName } from '../../staffContext'
import type { AppVersion } from '../../types'

const { data, error, loading, reload } = useJson<AppVersion[]>(() => '/api/ops/app-versions')
const drafts = reactive<Record<string, { latestVersion: string; minSupportedVersion: string }>>({})
const message = ref<{ kind: 'ok' | 'error'; text: string }>()

watch(data, (list) => {
  for (const v of list ?? []) {
    drafts[v.platform] = { latestVersion: v.latestVersion, minSupportedVersion: v.minSupportedVersion }
  }
})

async function save(platform: string) {
  message.value = undefined
  try {
    await putJson<AppVersion>(`/api/ops/app-versions/${platform}`, { ...drafts[platform], userId: currentStaff.value })
    message.value = { kind: 'ok', text: `${platform === 'IOS' ? 'iOS' : 'Android'} 버전 기준을 바꿨습니다.` }
    await reload()
  } catch (e) {
    message.value = { kind: 'error', text: e instanceof Error ? e.message : String(e) }
  }
}
</script>

<template>
  <h1>앱 버전 관리</h1>
  <p class="muted">
    앱은 켜질 때 서버에 버전을 묻습니다. 최소 지원 버전보다 낮으면 강제 업데이트 안내가 뜨고 앱을 쓸 수 없습니다.
    기준은 운영 팀장만 바꿀 수 있습니다. 지금 {{ staffName(currentStaff) }}(으)로 보고 있습니다.
  </p>
  <p v-if="message" :class="message.kind === 'ok' ? 'ok' : 'error'" role="status">{{ message.text }}</p>
  <LoadState :loading="loading" :error="error" />
  <table v-if="data" class="grid">
    <thead><tr><th>플랫폼</th><th>최신 버전</th><th>최소 지원 버전</th><th>변경 일시</th><th></th></tr></thead>
    <tbody>
      <tr v-for="v in data" :key="v.platform">
        <td>{{ v.platform === 'IOS' ? 'iOS' : 'Android' }}</td>
        <td><input v-if="drafts[v.platform]" v-model="drafts[v.platform].latestVersion" class="ver" :aria-label="`${v.platform} 최신 버전`" /></td>
        <td><input v-if="drafts[v.platform]" v-model="drafts[v.platform].minSupportedVersion" class="ver" :aria-label="`${v.platform} 최소 지원 버전`" /></td>
        <td>{{ dateTime(v.updatedAt) }}</td>
        <td><button type="button" @click="save(v.platform)">저장</button></td>
      </tr>
    </tbody>
  </table>
</template>
