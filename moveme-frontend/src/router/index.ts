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
    {
      path: '/persons/:id',
      name: 'PersonDetail',
      component: () => import('../views/PersonDetail.vue'),
    },
    {
      path: '/charts',
      name: 'Charts',
      component: () => import('../views/Charts.vue'),
    },
    {
      path: '/boards',
      name: 'Boards',
      component: () => import('../views/Boards.vue'),
    },
    {
      path: '/top250',
      name: 'Top250',
      component: () => import('../views/Top250.vue'),
    },
    {
      path: '/annual/:year?',
      name: 'Annual',
      component: () => import('../views/Annual.vue'),
    },
    {
      path: '/user/dashboard',
      name: 'UserDashboard',
      component: () => import('../views/user/UserDashboard.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('../views/admin/AdminDashboard.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/recommend/:id?',
      name: 'Recommend',
      component: () => import('../views/Recommend.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 有 token 但没有 userInfo 时，自动拉取用户信息
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
    }
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
