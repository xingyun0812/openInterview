import { http, type Result } from './http'

export type EvaluationSummary = {
  interviewId: number
  finalScore?: string
  humanAvgScore?: string
  submittedEvaluateCount: number
  totalEvaluateCount: number
  flowStatus: string
}

export async function getEvaluationSummary(interviewId: number) {
  const resp = await http.get<Result<EvaluationSummary>>('/api/v2/evaluations/summary', { params: { interviewId } })
  return resp.data.data
}

export async function listEvaluations(interviewId: number) {
  const resp = await http.get<Result<{ interviewId: number; evaluations: any[] }>>('/api/v2/evaluations', { params: { interviewId } })
  return resp.data.data
}

export async function draftEvaluation(payload: any) {
  const resp = await http.post<Result<any>>('/api/v2/evaluations/draft', payload)
  return resp.data.data
}

export async function submitEvaluation(payload: { interviewId: number; interviewerId: number }) {
  const resp = await http.post<Result<any>>('/api/v2/evaluations/submit', payload)
  return resp.data.data
}

export async function reviewEvaluation(payload: { interviewId: number; interviewResult: number; remark?: string }) {
  const resp = await http.post<Result<any>>('/api/v2/evaluations/review', payload)
  return resp.data.data
}

