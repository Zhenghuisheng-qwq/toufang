import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Landing from './Landing.vue'
import Order from './Order.vue'
import './style.css'

const router = createRouter({ history: createWebHistory(), routes: [
  { path: '/', component: Landing },
  { path: '/pages/:id', component: Landing },
  { path: '/orders/:no', component: Order },
  { path: '/:pathMatch(.*)*', component: Landing }
] })
createApp(App).use(router).mount('#app')
