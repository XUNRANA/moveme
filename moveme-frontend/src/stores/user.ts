import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

interface UserInfo {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl: string
  role: number
}

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)

  async function login(username: string, password: string) {
    const res: any = await request.post('/auth/login', { username, password })
    accessToken.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    localStorage.setItem('accessToken', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    await fetchUserInfo()
  }

  async function register(username: string, password: string, email?: string, nickname?: string) {
    await request.post('/auth/register', { username, password, email, nickname })
  }

  async function fetchUserInfo() {
    const res: any = await request.get('/users/me')
    userInfo.value = res.data
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchUserInfo,
    logout,
  }
})
