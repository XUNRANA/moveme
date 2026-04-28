import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue'),
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('../views/Home.vue'),
    },
    {
      path: '/movies/:id',
      name: 'MovieDetail',
      component: () => import('../views/MovieDetail.vue'),
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const isPublicRoute = to.path === '/' || to.path === '/login' || to.path === '/register' || to.path.startsWith('/movies/')
  const authRequired = !isPublicRoute

  if (authRequired && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
