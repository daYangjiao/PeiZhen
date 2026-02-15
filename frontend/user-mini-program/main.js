import App from './App'
import { createPinia } from 'pinia'

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false

// Vue 2 中使用 Pinia 需要额外配置
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
  
  // 配置 Pinia
  const pinia = createPinia()
  app.use(pinia)
  
  return {
    app
  }
}
// #endif