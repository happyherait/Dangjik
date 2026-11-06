<script setup lang="ts">
import { ref } from 'vue'
import { postJson, useJson } from '../../api'
import { currentStaff, staffName } from '../../staffContext'
import type { DutyAnswer } from '../../types'

interface Turn {
  question: string
  asker: string
  answer?: DutyAnswer
  error?: string
}

const EXAMPLES = [
  'INQ-0001 문의 확인해 주세요. 결제했는데 결제대기로 나온대요.',
  '결제대기로 남은 주문 있어?',
  'PRD-0006 상품 설명 좀 봐줘.',
]

const { data: mode } = useJson<{ mode: string }>(() => '/api/ops/chat/mode')
const question = ref('')
const turns = ref<Turn[]>([])
const busy = ref(false)

async function ask(text?: string) {
  const q = (text ?? question.value).trim()
  if (!q || busy.value) return
  const turn: Turn = { question: q, asker: currentStaff.value }
  turns.value.unshift(turn)
  question.value = ''
  busy.value = true
  try {
    turn.answer = await postJson<DutyAnswer>('/api/ops/chat', { message: q, user: currentStaff.value })
  } catch (e) {
    turn.error = e instanceof Error ? e.message : String(e)
  } finally {
    busy.value = false
    turns.value = [...turns.value]
  }
}
</script>

<template>
  <h1>
    AI 당직
    <span v-if="mode" class="tag" :class="mode.mode === 'claude' ? 'ok-tag' : 'warn'">
      {{ mode.mode === 'claude' ? 'Claude 연결됨' : '데모 모드' }}
    </span>
  </h1>
  <p class="muted">
    AI는 사실을 조회하고 런북 실행을 제안만 합니다. 실행은 운영 팀장이 런북 화면에서 승인해야 일어납니다.
  </p>
  <form class="ask" @submit.prevent="ask()">
    <textarea v-model="question" rows="2" placeholder="장애 상황이나 문의번호를 적어 주세요" aria-label="질문" />
    <button type="submit" :disabled="busy">{{ busy ? '조사 중…' : '물어보기' }}</button>
  </form>
  <div class="examples">
    <button v-for="e in EXAMPLES" :key="e" type="button" class="ghost" :disabled="busy" @click="ask(e)">{{ e }}</button>
  </div>

  <article v-for="(t, i) in turns" :key="turns.length - i" class="panel turn">
    <p class="q"><strong>{{ staffName(t.asker) }}</strong> {{ t.question }}</p>
    <p v-if="!t.answer && !t.error" class="muted">조사 중…</p>
    <p v-if="t.error" class="error" role="alert">{{ t.error }}</p>
    <template v-if="t.answer">
      <ol class="steps" aria-label="조회 과정">
        <li v-for="(s, j) in t.answer.steps" :key="j" :class="{ bad: s.error }">
          <code>{{ s.tool }}</code> {{ s.summary }}
        </li>
      </ol>
      <div class="answer">{{ t.answer.answer }}</div>
      <div v-if="t.answer.proposal?.runId" class="proposal">
        <strong>런북 제안 {{ t.answer.proposal.runId }}</strong>
        · 대상 {{ t.answer.proposal.plan.changes.length }}건 · 승인 대기
        <RouterLink :to="`/ops/runbooks/${t.answer.proposal.runId}`" class="link">승인 화면으로 →</RouterLink>
      </div>
    </template>
  </article>
</template>
