import request from '@/utils/request'

export function listLanding(query: Record<string, unknown>) {
  return request({ url: '/acq/landing/list', method: 'get', params: query })
}

export function getLanding(id: string) {
  return request({ url: '/acq/landing/' + id, method: 'get' })
}

export function addLanding(data: Record<string, unknown>) {
  return request({ url: '/acq/landing', method: 'post', data })
}

export function updateLanding(id: string, data: Record<string, unknown>) {
  return request({ url: '/acq/landing/' + id, method: 'put', data })
}

export function changeLandingStatus(id: string, status: number) {
  return request({ url: '/acq/landing/changeStatus', method: 'put', data: { id, status } })
}

export function delLanding(ids: string | string[]) {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request({ url: '/acq/landing/' + value, method: 'delete' })
}

export function landingOptions() {
  return request({ url: '/acq/landing/options', method: 'get' })
}
