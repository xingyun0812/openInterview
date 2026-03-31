import { http, type Result } from './http'

export type LoginRequest = { username: string; password: string }
export type AuthResponse = { token: string; refreshToken: string; tokenType: string; expiresInMs: number }

export async function login(req: LoginRequest) {
  const resp = await http.post<Result<AuthResponse>>('/api/auth/login', req)
  return resp.data.data
}

