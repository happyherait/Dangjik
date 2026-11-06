// 하루마켓 고객용 몰. 상품과 주문 데이터는 당직 API(Spring Boot)에서 가져온다.
const apiOrigin = process.env.DANGJIK_API_ORIGIN ?? 'http://localhost:18082'

export default defineNuxtConfig({
  compatibilityDate: '2026-11-01',
  devtools: { enabled: false },
  css: ['~/assets/main.css'],
  app: {
    head: {
      htmlAttrs: { lang: 'ko' },
      title: '하루마켓',
      meta: [{ name: 'description', content: '가상 쇼핑몰 하루마켓 (당직 프로토타입)' }],
    },
  },
  // 브라우저와 서버 렌더링 모두 같은 주소(/api)로 부르고, Nuxt 서버가 API로 전달한다
  routeRules: {
    '/api/**': { proxy: `${apiOrigin}/api/**` },
  },
})
