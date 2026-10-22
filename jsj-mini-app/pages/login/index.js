// pages/login/index.js
const app = getApp()

Page({
  data: {
    loading: false
  },

  onLoad() {
    // 已登录则直接跳首页
    if (app.globalData.token) {
      wx.reLaunch({ url: '/pages/index/index' })
    }
  },

  async onLogin() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      await app.doWxLogin()
      wx.reLaunch({ url: '/pages/index/index' })
    } catch (e) {
      wx.showToast({ title: '登录失败，请重试', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
