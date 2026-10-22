/**
 * 隔离 PoC：支付后数据库任务的领取、租约、未知结果。
 * 纯内存条件更新，不建表、不写生产库。
 */

export function insertTask(tasks, task) {
  const exists = tasks.some((item) => item.orderNo === task.orderNo && item.taskType === task.taskType)
  if (exists) {
    const error = new Error('duplicate orderNo+taskType')
    error.code = 'ER_DUP_ENTRY'
    throw error
  }
  tasks.push({
    status: 'PENDING',
    attempts: 0,
    leaseOwner: '',
    leaseUntil: 0,
    nextRunAt: 0,
    lastError: '',
    ...task
  })
}

export function claimTask(tasks, { id, owner, now, leaseMs }) {
  const task = tasks.find((item) => item.id === id)
  if (!task || task.nextRunAt > now) return false
  const leaseExpired = task.status === 'RUNNING' && task.leaseUntil <= now
  const claimable = task.status === 'PENDING' || leaseExpired
  if (!claimable) return false
  task.status = 'RUNNING'
  task.leaseOwner = owner
  task.leaseUntil = now + leaseMs
  return true
}

export function commitPaid(db) {
  const snapshot = {
    order: { ...db.order },
    tasks: db.tasks.map((item) => ({ ...item }))
  }
  try {
    if (db.order.payStatus === 'SUCCESS') return { created: false }
    db.order.payStatus = 'SUCCESS'
    for (const taskType of ['REPORT', 'CONTACT_WAY', 'AD_POSTBACK']) {
      insertTask(db.tasks, {
        id: `${db.order.orderNo}:${taskType}`,
        orderNo: db.order.orderNo,
        taskType,
        nextRunAt: 0
      })
    }
    return { created: true }
  } catch (error) {
    db.order = snapshot.order
    db.tasks = snapshot.tasks
    throw error
  }
}

export function settleTask(task, outcome, now) {
  if (task.status !== 'RUNNING') return false
  if (outcome === 'SUCCESS') {
    task.status = 'SUCCESS'
    task.leaseOwner = ''
    return true
  }
  if (outcome === 'FAIL') {
    task.status = 'PENDING'
    task.attempts += 1
    task.leaseOwner = ''
    task.leaseUntil = 0
    task.nextRunAt = now + Math.min(60_000 * 2 ** task.attempts, 30 * 60_000)
    task.lastError = 'downstream failed'
    return true
  }
  if (outcome === 'UNKNOWN') {
    task.status = 'UNKNOWN'
    task.leaseOwner = ''
    task.leaseUntil = 0
    task.lastError = 'downstream result unknown'
    return true
  }
  return false
}

export function resolveUnknown(task, confirmed, now) {
  if (task.status !== 'UNKNOWN') return false
  if (confirmed === 'SUCCESS') {
    task.status = 'SUCCESS'
    return true
  }
  task.status = 'PENDING'
  task.attempts += 1
  task.nextRunAt = now + 60_000
  return true
}
