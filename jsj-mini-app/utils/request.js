// 网络请求封装
const app = getApp()

const request = (url, data = {}, method = 'POST') => {
  return new Promise((resolve, reject) => {
    const headers = { 'content-type': 'application/json' }
    const token = app.globalData.token || wx.getStorageSync('token')
    if (token) {
      // 统一 Bearer 格式，与 admin-ui / uni-app 对齐
      headers.Authorization = `Bearer ${token}`
    }

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header: headers,
      success(res) {
        if (res.statusCode === 200) {
          const body = res.data
          if (body && body.code === 200) {
            resolve(body.data)
          } else if (body && body.code === 401) {
            handleUnauthorized()
            reject(body)
          } else {
            wx.showToast({ title: (body && body.msg) || '请求失败', icon: 'none' })
            reject(body || res)
          }
        } else if (res.statusCode === 401) {
          handleUnauthorized()
          reject(res)
        } else {
          reject(res)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络请求失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

let isRelogin = false

function handleUnauthorized() {
  if (isRelogin) return
  isRelogin = true
  try {
    app.globalData.token = ''
    app.globalData.userId = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userId')
  } catch (e) { /* ignore */ }
  wx.reLaunch({
    url: '/pages/login/index',
    complete: () => {
      setTimeout(() => { isRelogin = false }, 1500)
    }
  })
}

module.exports = {
  get: (url, data) => request(url, data, 'GET'),
  post: (url, data) => request(url, data, 'POST')
}
