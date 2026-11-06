<script setup lang="ts">
import { currentStaff, STAFF } from '../staffContext'
</script>

<template>
  <label class="picker">
    <span>직원으로 보기</span>
    <select v-model="currentStaff" aria-label="직원 선택">
      <option v-for="s in STAFF" :key="s.userId" :value="s.userId">{{ s.name }} · {{ s.title }}</option>
    </select>
  </label>
</template>
