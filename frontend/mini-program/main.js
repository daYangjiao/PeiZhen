import App from './App'
import { createPinia } from 'pinia'

// #ifdef H5
if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // Keep PWA registration best-effort only.
    })
  })
}
// #endif

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false

import { PiniaVuePlugin } from 'pinia'
Vue.use(PiniaVuePlugin)

const pinia = createPinia()

App.mpType = 'app'
const app = new Vue({
  pinia,
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  return { app }
}
// #endif
