<script setup lang="ts">
defineProps<{ loading: boolean; error?: string }>()
</script>

<template>
  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <p v-else-if="loading" class="muted">불러오는 중…</p>
</template>
