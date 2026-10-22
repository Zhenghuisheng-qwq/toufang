/**
 * 隔离 PoC：跨域报告使用短期一次性交换票据。
 * 长期凭证只以哈希形式留在服务端，不放进 URL。
 */

function randomId() {
  const bytes = new Uint8Array(18)
  if (globalThis.crypto && globalThis.crypto.getRandomValues) {
    globalThis.crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < bytes.length; i += 1) bytes[i] = Math.floor(Math.random() * 256)
  }
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

export function createTicketDesk({ now = () => Date.now(), ttlMs = 60_000 } = {}) {
  const codes = new Map()
  return {
    issue({ voucherHash, orderNo }) {
      if (!voucherHash || voucherHash.length < 16) {
        throw new Error('长期凭证必须先哈希，且不能为空')
      }
      const code = randomId()
      if (code === voucherHash) throw new Error('交换码不能等于长期凭证')
      codes.set(code, {
        voucherHash,
        orderNo,
        exp: now() + ttlMs,
        used: false
      })
      return code
    },
    exchange(code, at = now()) {
      const row = codes.get(code)
      if (!row || row.used || row.exp <= at) return null
      row.used = true
      return {
        sessionId: randomId(),
        orderNo: row.orderNo
      }
    }
  }
}

export function buildReportEntry(origin, exchangeCode) {
  const base = String(origin || '').replace(/\/$/, '')
  return `${base}/#/pages/h5/report?exchange=${encodeURIComponent(exchangeCode)}`
}

export function stripExchange(url) {
  const hashIndex = url.indexOf('#')
  if (hashIndex < 0) return url
  const hash = url.slice(hashIndex + 1)
  const [path] = hash.split('?')
  return `${url.slice(0, hashIndex)}#${path}`
}
