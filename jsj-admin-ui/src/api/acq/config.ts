import request from '@/utils/request'

export type ConfigKind = 'merchant' | 'wecom' | 'route' | 'ad'
export function configRequest(kind: ConfigKind, path = '', method = 'get', data?: Record<string, unknown>) {
  // Local saving locks prevent repeat clicks; never persist sensitive commands in sessionStorage.
  return request({ url: '/acq/' + kind + path, method, headers: { repeatSubmit: false }, ...(method === 'get' ? { params: data } : { data }) })
}
