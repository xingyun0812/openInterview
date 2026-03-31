import { http, type Result } from './http'

export type ExportTask = {
  taskId: number
  taskCode?: string
  taskStatus: number
  taskStatusLabel?: string
  exportType: number
  exportTypeLabel?: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
  fileHash?: string
  failReason?: string
  retryCount?: number
  stateFlow?: any
}

export async function createUnifiedExportTask(
  req:
    | { exportType: 0; candidateIds: number[]; jobCode: string }
    | { exportType: 1 | 2; interviewIds: number[] },
  idemKey: string,
) {
  const resp = await http.post<Result<ExportTask>>('/api/v1/export/task', req, { headers: { 'X-Idempotency-Key': idemKey } })
  return resp.data.data
}

export async function getExportTask(taskId: number) {
  const resp = await http.get<Result<ExportTask>>(`/api/v1/export/task/${taskId}`)
  return resp.data.data
}

export async function retryExportTask(taskId: number, idemKey: string) {
  const resp = await http.post<Result<any>>(`/api/v1/export/task/${taskId}/retry`, null, { headers: { 'X-Idempotency-Key': idemKey } })
  return resp.data.data
}

