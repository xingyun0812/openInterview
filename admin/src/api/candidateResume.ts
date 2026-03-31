import { http, type Result } from './http'

export type UploadResp = {
  candidateId: number
  resumeUrl: string
  mqEventCode?: string
  webhookEventCode?: string
}

export type ParseResp = {
  taskCode: string
  parseStatus: number
}

export type ParseResultResp = {
  candidateId: number
  parseStatus: number
  basicInfo?: any
  education?: any
  workExperience?: any
  skillTags?: any
  errorCode?: string
  failReason?: string
}

export type ScreenResp = {
  taskCode: string
  screenStatus: number
  reasonSummary?: string
}

export type ScreenResultResp = {
  candidateId: number
  jobCode: string
  screenStatus: number
  matchScore?: number
  recommendLevel?: number
  reasonSummary?: string
  reviewResult?: number
}

export async function uploadResume(candidateId: number, file: File, idemKey: string) {
  const fd = new FormData()
  fd.append('candidateId', String(candidateId))
  fd.append('resumeFile', file)
  const resp = await http.post<Result<UploadResp>>('/api/v1/candidate/resume/upload', fd, {
    headers: { 'X-Idempotency-Key': idemKey, 'Content-Type': 'multipart/form-data' },
  })
  return resp.data.data
}

export async function parseResume(candidateId: number, resumeUrl: string, idemKey: string) {
  const resp = await http.post<Result<ParseResp>>(
    '/api/v1/candidate/resume/parse',
    { candidateId, resumeUrl },
    { headers: { 'X-Idempotency-Key': idemKey } },
  )
  return resp.data.data
}

export async function getParseResult(candidateId: number) {
  const resp = await http.get<Result<ParseResultResp>>(`/api/v1/candidate/resume/parse/result/${candidateId}`)
  return resp.data.data
}

export async function screenResume(candidateId: number, jobCode: string, idemKey: string) {
  const resp = await http.post<Result<ScreenResp>>(
    '/api/v1/candidate/resume/screen',
    { candidateId, jobCode },
    { headers: { 'X-Idempotency-Key': idemKey } },
  )
  return resp.data.data
}

export async function getScreenResult(candidateId: number, jobCode: string) {
  const resp = await http.get<Result<ScreenResultResp>>(`/api/v1/candidate/resume/screen/result/${candidateId}`, {
    params: { jobCode },
  })
  return resp.data.data
}

export async function reviewScreen(
  candidateId: number,
  jobCode: string,
  reviewResult: number,
  reviewComment: string,
  idemKey: string,
) {
  const resp = await http.post<Result<any>>(
    '/api/v1/candidate/resume/screen/review',
    { candidateId, jobCode, reviewResult, reviewComment },
    { headers: { 'X-Idempotency-Key': idemKey } },
  )
  return resp.data.data
}

