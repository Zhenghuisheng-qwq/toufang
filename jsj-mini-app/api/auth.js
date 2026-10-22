// api/auth.js - 认证接口
const request = require('../utils/request.js')

/**
 * 微信小程序登录：wx.login → code → 后端换 token
 * @returns Promise<{token, userId, nickname, avatar}>
 */
const wxLogin = (code, profile = {}) =>
  request.post('/api/auth/wx-login', {
    code,
    nickname: profile.nickName || '',
    avatar: profile.avatarUrl || ''
  })

/** 获取当前登录用户 */
const currentUser = () => request.post('/api/auth/current-user')

/** 退出登录（后端会删 token + 断 WebSocket） */
const logout = () => request.post('/api/auth/logout')

module.exports = { wxLogin, currentUser, logout }
