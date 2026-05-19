import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import { vAnimate } from './directives/vAnimate'
import App from './App.vue'
import './assets/styles/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.directive('animate', vAnimate)
app.mount('#app')
