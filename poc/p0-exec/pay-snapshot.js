/**
 * 隔离 PoC：回调核对与商户快照。
 * 只验证本地规则，不代表微信支付联调通过，也不标记真实支付成功。
 */

export function createOrder(fields) {
  return {
    payStatus: 'UNPAID',
    ...fields
  }
}

export function applyNotify(order, notice) {
  if (notice.mchId !== order.mchId) return 'REJECT_MERCHANT'
  if (notice.appId !== order.appId) return 'REJECT_APP'
  if (notice.outTradeNo !== order.orderNo) return 'REJECT_ORDER'
  if (notice.amount !== order.amount) return 'REJECT_AMOUNT'
  if (notice.tradeState !== 'SUCCESS') return 'IGNORE_STATE'
  if (order.payStatus === 'SUCCESS') return 'IDEMPOTENT'
  order.payStatus = 'SUCCESS'
  return 'ACCEPTED'
}

export function switchActiveMerchant(config, merchant) {
  config.activeMchId = merchant.mchId
  config.activeAppId = merchant.appId
}

export function decidePostback(order, click) {
  if (!order || order.payStatus !== 'SUCCESS') return 'SKIP_UNPAID'
  if (!click || !click.clickId) return 'SKIP_NO_CLICK'
  return 'SEND'
}
