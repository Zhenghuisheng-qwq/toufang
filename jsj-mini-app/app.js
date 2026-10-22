// app.js
const config = require('./config.js')
const { wxLogin } = require('./api/auth.js')

App({
  globalData: {
    userInfo: null,
    userId: '',
    token: '',
    baseUrl: config.baseUrl
  },

  onLaunch() {
    // 恢复本地登录态
    const token = wx.getStorageSync('token')
    const userId = wx.getStorageSync('userId')
    if (token) this.globalData.token = token
    if (userId) this.globalData.userId = userId
  },

  /**
   * 执行微信静默登录 —— 在登录按钮点击后调用
   * 返回 Promise，成功则全局态已写入 storage
   */
  doWxLogin() {
    const app = this
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (!res.code) {
            reject(new Error('wx.login 失败: ' + res.errMsg))
            return
          }
          wxLogin(res.code, {})
            .then((loginResp) => {
              app.globalData.token = loginResp.token
              app.globalData.userId = loginResp.userId
              app.globalData.userInfo = {
                nickname: loginResp.nickname,
                avatar: loginResp.avatar
              }
              wx.setStorageSync('token', loginResp.token)
              wx.setStorageSync('userId', loginResp.userId)
              resolve(loginResp)
            })
            .catch(reject)
        },
        fail: reject
      })
    })
  },

  /** 主动登出 —— 通知服务端删 token + 清本地 */
  doLogout() {
    const { logout } = require('./api/auth.js')
    return logout()
      .catch(() => { /* 请求失败也继续清本地 */ })
      .then(() => {
        this.globalData.token = ''
        this.globalData.userId = ''
        this.globalData.userInfo = null
        wx.removeStorageSync('token')
        wx.removeStorageSync('userId')
      })
  }
})
