import request from '@/utils/request'

export function templateOptions(keyword = '') {
  return request({ url: '/acq/template/options', method: 'get', params: { keyword } })
}
export function previewTemplate(data: Record<string, unknown>) {
  return request({ url: '/acq/template/preview', method: 'post', data, headers: { repeatSubmit: false } })
}

export function listTemplate(query: Record<string, unknown>) {
  return request({ url: '/acq/template/list', method: 'get', params: query })
}

export function getTemplate(id: string) {
  return request({ url: '/acq/template/' + id, method: 'get' })
}

export function addTemplate(data: Record<string, unknown>) {
  return request({ url: '/acq/template', method: 'post', data })
}

export function updateTemplate(id: string, data: Record<string, unknown>) {
  return request({ url: '/acq/template/' + id, method: 'put', data })
}

export function changeTemplateStatus(id: string, status: number) {
  return request({ url: '/acq/template/changeStatus', method: 'put', data: { id, status } })
}

export function delTemplate(ids: string | string[]) {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request({ url: '/acq/template/' + value, method: 'delete' })
}

export function createDraft(id: string, data: Record<string, unknown>) {
  return request({ url: '/acq/template/' + id + '/draft', method: 'post', data })
}

export function updateDraft(versionId: string, data: Record<string, unknown>) {
  return request({ url: '/acq/template/version/' + versionId, method: 'put', data })
}

export function publishVersion(versionId: string) {
  return request({ url: '/acq/template/version/' + versionId + '/publish', method: 'post' })
}

export function delDraft(versionId: string) {
  return request({ url: '/acq/template/version/' + versionId, method: 'delete' })
}
