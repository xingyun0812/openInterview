import { http, type Result } from './http'

export type InterviewPlan = {
  id: number
  interviewCode: string
  candidateId: number
  applyPosition: string
  interviewRound: string
  interviewType: number
  templateId: number
  interviewStartTime: string
  interviewEndTime: string
  interviewRoomId?: string
  interviewRoomLink?: string
  hrUserId?: number
  interviewerIds?: string
  interviewStatus?: number
  interviewResult?: number
  finalScore?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export type InterviewPlanPage = { records: InterviewPlan[]; total: number; current: number; size: number }

export type InterviewPlanCreate = {
  candidateId: number
  applyPosition: string
  interviewRound: string
  interviewType: number
  templateId: number
  interviewStartTime: string
  interviewEndTime: string
  interviewRoomId?: string
  interviewRoomLink?: string
  hrUserId?: number
  interviewerIds?: string
}

export type InterviewPlanUpdate = Partial<InterviewPlanCreate> & { remark?: string }

export async function pageInterviewPlans(page: number, size: number, status?: number) {
  const resp = await http.get<Result<InterviewPlanPage>>('/api/interview-plans', { params: { page, size, status } })
  return resp.data.data
}

export async function createInterviewPlan(req: InterviewPlanCreate, idemKey: string) {
  const resp = await http.post<Result<InterviewPlan>>('/api/interview-plans', req, { headers: { 'X-Idempotency-Key': idemKey } })
  return resp.data.data
}

export async function getInterviewPlanById(id: number) {
  const resp = await http.get<Result<InterviewPlan>>(`/api/interview-plans/${id}`)
  return resp.data.data
}

export async function getInterviewPlanByCode(code: string) {
  const resp = await http.get<Result<InterviewPlan>>(`/api/interview-plans/code/${code}`)
  return resp.data.data
}

export async function updateInterviewPlan(id: number, req: InterviewPlanUpdate) {
  const resp = await http.put<Result<InterviewPlan>>(`/api/interview-plans/${id}`, req)
  return resp.data.data
}

export async function startInterviewPlan(id: number) {
  const resp = await http.post<Result<InterviewPlan>>(`/api/interview-plans/${id}/start`)
  return resp.data.data
}

export async function completeInterviewPlan(id: number) {
  const resp = await http.post<Result<InterviewPlan>>(`/api/interview-plans/${id}/complete`)
  return resp.data.data
}

export async function cancelInterviewPlan(id: number) {
  const resp = await http.post<Result<InterviewPlan>>(`/api/interview-plans/${id}/cancel`)
  return resp.data.data
}

