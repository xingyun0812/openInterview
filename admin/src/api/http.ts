import axios, { type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'

export type Result<T> = {
  code: number
  msg: string
  traceId?: string
  bizCode?: string
  data: T
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20_000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const contentType = String(resp.headers?.['content-type'] ?? '')
    if (!contentType.includes('application/json')) return resp

    const body = resp.data as Result<unknown>
    // 后端约定：成功码为 200；历史/部分 mock 可能用 0
    if (body && typeof body.code === 'number' && body.code !== 0 && body.code !== 200) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '业务错误'))
    }
    return resp
  },
  async (err: AxiosError) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      const { router } = await import('../router')
      if (router.currentRoute.value.path !== '/login') {
        await router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      return Promise.reject(err)
    }

    const msg =
      (err.response?.data as any)?.msg ||
      (err.response?.data as any)?.message ||
      err.message ||
      '网络错误'
    ElMessage.error(msg)
    return Promise.reject(err)
  },
)

