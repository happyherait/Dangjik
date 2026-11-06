import { ref } from 'vue'

// 프로토타입이라 로그인 대신 화면에서 직원을 고른다 (시드 데이터의 직원)
export const STAFF = [
  { userId: 'ops.kim', name: '김운영', role: 'OPS_ENGINEER', title: '운영 담당' },
  { userId: 'lead.park', name: '박팀장', role: 'OPS_LEAD', title: '운영 팀장' },
  { userId: 'cs.lee', name: '이상담', role: 'CS_AGENT', title: '상담원' },
] as const

export const currentStaff = ref<string>('ops.kim')

export function staffName(userId: string): string {
  const s = STAFF.find((x) => x.userId === userId)
  return s ? `${s.name}(${s.title})` : userId
}
