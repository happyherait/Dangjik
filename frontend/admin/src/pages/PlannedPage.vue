<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <h1>{{ route.meta.title }}</h1>
  <p class="planned">아직 준비 중인 화면입니다.</p>
</template>
