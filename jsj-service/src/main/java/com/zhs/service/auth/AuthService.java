package com.zhs.service.auth;

import com.zhs.service.dto.auth.LoginResult;
import com.zhs.service.dto.auth.PhoneLoginCommand;
import com.zhs.service.dto.auth.UserProfileDto;
import com.zhs.service.dto.auth.WxLoginCommand;

/**
 * 认证业务 Service（数据库 + 编排）
 */
public interface AuthService {

    LoginResult wxLogin(WxLoginCommand command);

    void sendPhoneCode(String phone);

    LoginResult phoneLogin(PhoneLoginCommand command);

    UserProfileDto getCurrentUser(Long userId);

    void logout(String token);

    /**
     * 按 token 查询仍有效的登录会话用户 ID；不存在或已过期返回 null。
     */
    Long findActiveUserIdByToken(String token);
}
