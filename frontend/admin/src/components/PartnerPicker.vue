<script setup lang="ts">
import { useJson } from '../api'
import { currentPartnerId } from '../partnerContext'
import type { Partner } from '../types'

const { data: partners } = useJson<Partner[]>(() => '/api/partner')
</script>

<template>
  <label class="picker">
    <span>입점사로 보기</span>
    <select v-model="currentPartnerId" aria-label="입점사 선택">
      <option v-for="p in partners ?? []" :key="p.partnerId" :value="p.partnerId">
        {{ p.name }} ({{ p.partnerId }})
      </option>
    </select>
  </label>
</template>
