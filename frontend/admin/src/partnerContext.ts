import { ref } from 'vue'

// 프로토타입이라 로그인 대신 화면에서 입점사를 고른다
export const PARTNER_USERS: Record<string, string> = {
  'P-001': 'p001.han',
  'P-002': 'p002.yoon',
  'P-003': 'p003.demo',
}

export const currentPartnerId = ref('P-001')

export function currentPartnerUser(): string {
  return PARTNER_USERS[currentPartnerId.value] ?? 'partner'
}
