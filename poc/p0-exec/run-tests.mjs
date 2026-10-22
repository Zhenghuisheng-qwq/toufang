import assert from 'node:assert/strict'
import test from 'node:test'
import { isAdH5Route, routeFromLaunchOptions, routeFromLocationHash, shouldBootstrapUser } from './h5-route.js'
import { assertSameOriginApi, createH5Client } from './h5-request.js'
import { buildReportEntry, createTicketDesk, stripExchange } from './report-ticket.js'
import { claimTask, commitPaid, insertTask, resolveUnknown, settleTask } from './task-lease.js'
import { applyNotify, createOrder, decidePostback, switchActiveMerchant } from './pay-snapshot.js'

test('onLaunch options.path 与 hash 都能识别广告页，广告页不 bootstrap', () => {
  const fromOptions = routeFromLaunchOptions({ path: 'pages/h5/report', query: { exchange: 'abc' } })
  const fromHash = routeFromLocationHash('#/pages/h5/report?exchange=abc')
  assert.equal(fromOptions, 'pages/h5/report')
  assert.equal(fromHash, 'pages/h5/report')
  assert.equal(shouldBootstrapUser(fromOptions), false)
  assert.equal(shouldBootstrapUser('pages/login/index'), true)
  assert.equal(shouldBootstrapUser('pages/index/index'), true)
  assert.equal(isAdH5Route('pages/h5'), true)
})

test('独立请求不携带 jsj_token，401 不跳登录，拒绝占位域名', async () => {
  const calls = []
  const client = createH5Client({
    baseUrl: '/api',
    transport: async (req) => {
      calls.push(req)
      return { status: 401, data: { code: 401, message: '未登录' } }
    }
  })
  const result = await client.request('/h5/report', { ticket: 'once' })
  assert.equal(result.redirectedToLogin, false)
  assert.equal(result.unauthorized, true)
  assert.equal(calls[0].url, '/api/h5/report')
  assert.equal(calls[0].headers.Authorization, undefined)
  assert.equal(calls[0].headers['X-Report-Ticket'], 'once')
  assert.throws(() => assertSameOriginApi('https://api.yoursite.com'), /占位域名|同源/)
  assert.throws(() => assertSameOriginApi('http://localhost:8081'), /同源/)
})

test('报告入口只用一次性交换码，用过即失效', () => {
  let clock = 1_000
  const desk = createTicketDesk({ now: () => clock, ttlMs: 1_000 })
  const voucherHash = 'a'.repeat(32)
  const code = desk.issue({ voucherHash, orderNo: 'NO1' })
  const entry = buildReportEntry('https://report.example', code)
  assert.equal(entry.includes(voucherHash), false)
  assert.match(entry, /#\/pages\/h5\/report\?exchange=/)
  const session = desk.exchange(code)
  assert.ok(session.sessionId)
  assert.equal(session.orderNo, 'NO1')
  assert.equal(desk.exchange(code), null)
  assert.equal(stripExchange(entry), 'https://report.example/#/pages/h5/report')
  clock = 5_000
  const expired = desk.issue({ voucherHash, orderNo: 'NO2' })
  assert.equal(desk.exchange(expired, clock + 2_000), null)
})

test('支付事务同时写三条任务，失败全部回滚；重复支付不新增任务', () => {
  const db = { order: { orderNo: 'NO1', payStatus: 'UNPAID' }, tasks: [], failTaskInsert: false }
  const first = commitPaid(db)
  assert.equal(first.created, true)
  assert.deepEqual(db.tasks.map((item) => item.taskType), ['REPORT', 'CONTACT_WAY', 'AD_POSTBACK'])
  const second = commitPaid(db)
  assert.equal(second.created, false)
  assert.equal(db.tasks.length, 3)

  const broken = { order: { orderNo: 'NO2', payStatus: 'UNPAID' }, tasks: [] }
  insertTask(broken.tasks, { id: 'seed', orderNo: 'NO2', taskType: 'REPORT' })
  assert.throws(() => commitPaid(broken), /duplicate/)
  assert.equal(broken.order.payStatus, 'UNPAID')
  assert.equal(broken.tasks.length, 1)
})

test('并发领取只有一个成功，租约过期后可恢复，未知结果不立即重发', () => {
  const tasks = []
  insertTask(tasks, { id: 't1', orderNo: 'NO1', taskType: 'AD_POSTBACK', nextRunAt: 0 })
  assert.equal(claimTask(tasks, { id: 't1', owner: 'a', now: 10, leaseMs: 100 }), true)
  assert.equal(claimTask(tasks, { id: 't1', owner: 'b', now: 20, leaseMs: 100 }), false)
  assert.equal(claimTask(tasks, { id: 't1', owner: 'b', now: 110, leaseMs: 100 }), true)
  assert.equal(tasks[0].leaseOwner, 'b')

  assert.equal(settleTask(tasks[0], 'UNKNOWN', 200), true)
  assert.equal(claimTask(tasks, { id: 't1', owner: 'c', now: 99999, leaseMs: 100 }), false)
  assert.equal(resolveUnknown(tasks[0], 'RETRY', 300), true)
  assert.equal(claimTask(tasks, { id: 't1', owner: 'c', now: 300, leaseMs: 100 }), false)
  assert.equal(claimTask(tasks, { id: 't1', owner: 'c', now: 60_300, leaseMs: 100 }), true)
})

test('三类任务独立补偿，历史商户快照不随切换改变', () => {
  const db = { order: createOrder({ orderNo: 'NO9', mchId: 'mch-a', appId: 'app-a', amount: 100 }), tasks: [] }
  commitPaid(db)
  const report = db.tasks.find((item) => item.taskType === 'REPORT')
  const qr = db.tasks.find((item) => item.taskType === 'CONTACT_WAY')
  const ad = db.tasks.find((item) => item.taskType === 'AD_POSTBACK')
  claimTask(db.tasks, { id: report.id, owner: 'w', now: 1, leaseMs: 50 })
  settleTask(report, 'FAIL', 1)
  assert.equal(qr.status, 'PENDING')
  assert.equal(ad.status, 'PENDING')
  assert.equal(report.status, 'PENDING')

  const config = { activeMchId: 'mch-a', activeAppId: 'app-a' }
  switchActiveMerchant(config, { mchId: 'mch-b', appId: 'app-b' })
  assert.equal(db.order.mchId, 'mch-a')
  assert.equal(applyNotify(db.order, {
    mchId: 'mch-b', appId: 'app-b', outTradeNo: 'NO9', amount: 100, tradeState: 'SUCCESS'
  }), 'REJECT_MERCHANT')
  assert.equal(db.order.payStatus, 'SUCCESS')
  assert.equal(db.order.mchId, 'mch-a')
  assert.equal(applyNotify(db.order, {
    mchId: 'mch-a', appId: 'app-a', outTradeNo: 'NO9', amount: 100, tradeState: 'SUCCESS'
  }), 'IDEMPOTENT')

  const fresh = createOrder({ orderNo: 'NO10', mchId: 'mch-a', appId: 'app-a', amount: 100 })
  assert.equal(applyNotify(fresh, {
    mchId: 'mch-a', appId: 'app-a', outTradeNo: 'NO10', amount: 1, tradeState: 'SUCCESS'
  }), 'REJECT_AMOUNT')
  assert.equal(fresh.payStatus, 'UNPAID')
  assert.equal(applyNotify(fresh, {
    mchId: 'mch-a', appId: 'app-a', outTradeNo: 'NO10', amount: 100, tradeState: 'SUCCESS'
  }), 'ACCEPTED')
  assert.equal(applyNotify(fresh, {
    mchId: 'mch-a', appId: 'app-a', outTradeNo: 'NO10', amount: 100, tradeState: 'SUCCESS'
  }), 'IDEMPOTENT')
})

test('无点击标识不回传，未支付不回传，平台结果互不代替', () => {
  const paid = { payStatus: 'SUCCESS' }
  assert.equal(decidePostback(paid, null), 'SKIP_NO_CLICK')
  assert.equal(decidePostback({ payStatus: 'UNPAID' }, { clickId: 'gdt-1' }), 'SKIP_UNPAID')
  const platforms = {
    tencent: decidePostback(paid, { clickId: 'gdt-1', platform: 'tencent' }),
    baidu: 'NOT_RUN',
    kuaishou: 'NOT_RUN'
  }
  assert.equal(platforms.tencent, 'SEND')
  assert.notEqual(platforms.baidu, platforms.tencent)
  assert.notEqual(platforms.kuaishou, platforms.tencent)
})
