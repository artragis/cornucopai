import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import HomePage from './HomePage.vue'
import AtelierDetailPage from './pages/AtelierDetailPage.vue'

// T024 (US4, spec 006, R-049): dedicated routes — `/` home, `/ateliers/:id` detail page.
const routes: RouteRecordRaw[] = [
  { path: '/', component: HomePage },
  { path: '/ateliers/:id', component: AtelierDetailPage, props: true }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})
