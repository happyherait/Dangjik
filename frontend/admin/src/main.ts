import { createApp } from 'vue'
import App from './App.vue'
import { createAppRouter } from './router'
import './style.css'

// 공개 데모(GitHub Pages)에서는 서버 없이 브라우저 안의 가짜 API로 동작한다
if (import.meta.env.VITE_DEMO === 'true') {
  const { installDemoApi } = await import('./demo/server')
  installDemoApi()
}

createApp(App).use(createAppRouter()).mount('#app')
