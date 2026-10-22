/**
 * 隔离 PoC：广告 H5 独立请求。
 * 不读取 jsj_token，401 不跳转登录页，只允许同源 /api。
 */

const PLACEHOLDER_HOST = 'yoursite.com'

export function assertSameOriginApi(baseUrl) {
  if (typeof baseUrl !== 'string' || !baseUrl.startsWith('/api')) {
    throw new Error('H5 API 必须使用同源 /api 前缀')
  }
  if (baseUrl.includes(PLACEHOLDER_HOST) || baseUrl.startsWith('http://') || baseUrl.startsWith('https://')) {
    throw new Error('H5 API 不能使用占位域名或绝对主机')
  }
}

export function createH5Client({ baseUrl, transport }) {
  assertSameOriginApi(baseUrl)
  return {
    async request(path, { ticket } = {}) {
      const headers = {
        'content-type': 'application/json',
        'X-Client-Type': 'uni-h5-ad'
      }
      if (ticket) headers['X-Report-Ticket'] = ticket
      const res = await transport({
        url: baseUrl + path,
        headers
      })
      if (res.status === 401 || (res.data && res.data.code === 401)) {
        return { ok: false, unauthorized: true, redirectedToLogin: false }
      }
      return {
        ok: res.status === 200 && (!res.data || res.data.code === undefined || res.data.code === 200),
        unauthorized: false,
        redirectedToLogin: false,
        data: res.data
      }
    }
  }
}
