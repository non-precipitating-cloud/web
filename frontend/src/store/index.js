import { createPinia, defineStore } from 'pinia'

const pinia = createPinia()
export default pinia

/**
 * 用户状态存储
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    phone: localStorage.getItem('phone') || '',
    role: parseInt(localStorage.getItem('userRole') || '0'),
    avatar: localStorage.getItem('avatar') || ''
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.role === 1
  },

  actions: {
    setLogin(token, userInfo) {
      this.token = token
      this.userId = userInfo.id
      this.username = userInfo.username
      this.nickname = userInfo.nickname
      this.phone = userInfo.phone || ''
      this.role = userInfo.role
      this.avatar = userInfo.avatar || ''

      localStorage.setItem('token', token)
      localStorage.setItem('userId', userInfo.id)
      localStorage.setItem('username', userInfo.username)
      localStorage.setItem('nickname', userInfo.nickname || '')
      localStorage.setItem('phone', userInfo.phone || '')
      localStorage.setItem('userRole', userInfo.role)
      localStorage.setItem('avatar', userInfo.avatar || '')
    },

    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.nickname = ''
      this.phone = ''
      this.role = 0
      this.avatar = ''

      localStorage.clear()
    },

    updateAvatar(url) {
      this.avatar = url
      localStorage.setItem('avatar', url)
    },

    updateProfile(info) {
      if (info.nickname !== undefined) {
        this.nickname = info.nickname
        localStorage.setItem('nickname', info.nickname)
      }
      if (info.phone !== undefined) {
        this.phone = info.phone
        localStorage.setItem('phone', info.phone)
      }
      if (info.avatar !== undefined) {
        this.avatar = info.avatar
        localStorage.setItem('avatar', info.avatar)
      }
    }
  }
})

/**
 * 点餐车状态存储
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0
  }),

  actions: {
    setCount(count) {
      this.count = count
    },
    increment() {
      this.count++
    },
    decrement() {
      if (this.count > 0) this.count--
    }
  }
})
