import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface ReportStatus {
  orderNo: string
  payStatus: number
  templateId?: string
  templateVersionId?: string
  reportId?: string
  reportStatus?: number
  generatedTime?: string
  taskId?: string
  taskStatus?: number
  attemptCount?: number
  leaseUntil?: string
  lastError?: string
}
const path = (orderNo: string) => '/acq/report/' + encodeURIComponent(orderNo)
export const getReportStatus = (orderNo: string): Promise<AjaxResult<ReportStatus>> => request({ url: path(orderNo) })
export const getReportContent = (orderNo: string): Promise<AjaxResult<string>> => request({ url: path(orderNo) + '/content' })
export const retryReport = (orderNo: string): Promise<AjaxResult<ReportStatus>> => request({
  url: path(orderNo) + '/retry', method: 'post', headers: { repeatSubmit: false }
})
