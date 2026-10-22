// pages/index/index.js
const app = getApp()
const { currentUser, logout: apiLogout } = require('../../api/auth.js')

Page({
  data: {
    nickname: '',
    avatar: ''
  },

  onLoad() {
    if (!app.globalData.token) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.fetchUser()
  },

  fetchUser() {
    currentUser()
      .then(u => this.setData({ nickname: u.nickname || '未命名', avatar: u.avatar || '' }))
      .catch(() => { /* request.js 已处理 401 */ })
  },

  onLogout() {
    wx.showModal({
      title: '提示',
      content: '确认退出登录？',
      success: (res) => {
        if (!res.confirm) return
        app.doLogout().then(() => {
          wx.reLaunch({ url: '/pages/login/index' })
        })
      }
    })
  }
})
