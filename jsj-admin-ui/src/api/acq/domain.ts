import request from '@/utils/request'

export function listDomain(query: Record<string, unknown>) {
  return request({ url: '/acq/domain/list', method: 'get', params: query })
}

export function getDomain(id: string) {
  return request({ url: '/acq/domain/' + id, method: 'get' })
}

export function addDomain(data: Record<string, unknown>) {
  return request({ url: '/acq/domain', method: 'post', data })
}

export function updateDomain(id: string, data: Record<string, unknown>) {
  return request({ url: '/acq/domain/' + id, method: 'put', data })
}

export function changeDomainStatus(id: string, status: number) {
  return request({ url: '/acq/domain/changeStatus', method: 'put', data: { id, status } })
}

export function delDomain(ids: string | string[]) {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request({ url: '/acq/domain/' + value, method: 'delete' })
}

export function linkLanding(id: string, landingPageId: string) {
  return request({ url: '/acq/domain/' + id + '/landing', method: 'post', data: { landingPageId } })
}

export function unlinkLanding(id: string, landingPageId: string) {
  return request({ url: '/acq/domain/' + id + '/landing/' + landingPageId, method: 'delete' })
}

export function linkMerchant(id: string, payMerchantId: string) {
  return request({ url: '/acq/domain/' + id + '/merchant', method: 'post', data: { payMerchantId } })
}

export function unlinkMerchant(id: string, payMerchantId: string) {
  return request({ url: '/acq/domain/' + id + '/merchant/' + payMerchantId, method: 'delete' })
}

export function merchantOptions() {
  return request({ url: '/acq/domain/merchant-options', method: 'get' })
}
