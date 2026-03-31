import { defineStore } from 'pinia'

const LS_TOKEN = 'oi_admin_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(LS_TOKEN) || '',
    username: '',
    dark: localStorage.getItem('oi_admin_dark') === '1',
  }),
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(LS_TOKEN, token)
    },
    setDark(enabled: boolean) {
      this.dark = enabled
      localStorage.setItem('oi_admin_dark', enabled ? '1' : '0')
      document.documentElement.classList.toggle('dark', enabled)
    },
    bootstrap() {
      document.documentElement.classList.toggle('dark', this.dark)
    },
    logout() {
      this.token = ''
      localStorage.removeItem(LS_TOKEN)
    },
  },
})

