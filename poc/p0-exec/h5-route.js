/**
 * 隔离 PoC：广告 H5 与原登录启动流程的路由判断。
 * 不接入正式 App.vue。正式启动仍会无条件 bootstrap。
 */

export function normalizeRoutePath(path) {
  if (!path) return ''
  const noHash = String(path).replace(/^#/, '')
  const noQuery = noHash.split('?')[0]
  return noQuery.replace(/^\//, '')
}

/** uni-app H5：onLaunch(options.path) 在 router.isReady() 之后给出，无前导斜杠。 */
export function routeFromLaunchOptions(options) {
  return normalizeRoutePath(options && options.path)
}

/** H5 兜底：hash 路由的路径不经过服务器，可直接从 location.hash 读取。 */
export function routeFromLocationHash(hash) {
  return normalizeRoutePath(hash)
}

export function isAdH5Route(path) {
  const route = normalizeRoutePath(path)
  return route === 'pages/h5' || route.startsWith('pages/h5/')
}

export function shouldBootstrapUser(path) {
  return !isAdH5Route(path)
}
