package com.zhs.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.auth.AuthService;
import com.zhs.service.dto.auth.LoginResult;
import com.zhs.service.dto.auth.PhoneLoginCommand;
import com.zhs.service.dto.auth.UserProfileDto;
import com.zhs.service.dto.auth.WxLoginCommand;
import com.zhs.service.mapper.UserLoginSessionMapper;
import com.zhs.service.mapper.UserMapper;
import com.zhs.service.mapper.UserSensitiveInfoMapper;
import com.zhs.service.model.User;
import com.zhs.service.model.UserLoginSession;
import com.zhs.service.model.UserSensitiveInfo;
import com.zhs.service.support.AuthTokenSupport;
import com.zhs.service.support.WeChatLoginSupport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@ConditionalOnBean({AuthTokenSupport.class, WeChatLoginSupport.class})
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserSensitiveInfoMapper sensitiveInfoMapper;

    @Resource
    private UserLoginSessionMapper loginSessionMapper;

    @Resource
    private WeChatLoginSupport weChatLoginSupport;

    @Resource
    private AuthTokenSupport authTokenSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResult wxLogin(WxLoginCommand command) {
        WeChatLoginSupport.WeChatSession wx = weChatLoginSupport.getSessionByCode(command.getCode());
        String openId = wx.openId();
        String unionId = wx.unionId();
        log.info("[wx-login] code2Session 完成 openId={} unionId.present={}", openId, unionId != null);

        User user = findOrCreateByOpenId(openId, unionId, command.getNickname(), command.getAvatar());
        String token = issueToken(user.getId());
        log.info("[wx-login] 登录成功 userId={}", user.getId());
        return toLoginResult(token, user);
    }

    @Override
    public void sendPhoneCode(String phone) {
        // No SMS provider is wired here. Never acknowledge delivery in a non-development profile.
        if (!authTokenSupport.isDevProfile()) {
            throw new BusinessException("短信发送能力尚未配置");
        }
        if (!authTokenSupport.tryAcquireSmsSendLimit(phone)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        authTokenSupport.storeSmsCode(phone, code);
        log.info("[sendPhoneCode][dev] 已生成开发验证码，未发送短信");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResult phoneLogin(PhoneLoginCommand command) {
        String phone = command.getPhone();
        String expected = authTokenSupport.consumeSmsCode(phone);
        if (!StringUtils.hasText(expected)) {
            throw new BusinessException("验证码已过期或已使用，请重新获取");
        }
        if (!expected.equals(command.getCode())) {
            throw new BusinessException("验证码错误");
        }

        User user = findOrCreateByPhone(phone);
        String token = issueToken(user.getId());
        log.info("[phone-login] 登录成功 userId={}", user.getId());
        return toLoginResult(token, user);
    }

    @Override
    public UserProfileDto getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete() == 1) {
            throw new BusinessException("用户不存在");
        }
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getId());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        return dto;
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        authTokenSupport.removeTokenAndGetUserId(token);
        log.info("[logout] token 已清除");
    }

    @Override
    public Long findActiveUserIdByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        UserLoginSession session = loginSessionMapper.selectOne(
                new LambdaQueryWrapper<UserLoginSession>()
                        .eq(UserLoginSession::getToken, token)
                        .eq(UserLoginSession::getStatus, 1)
                        .eq(UserLoginSession::getIsDelete, 0)
                        .last("limit 1"));
        if (session == null || session.getExpireTime() == null
                || session.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        return session.getUserId();
    }

    private User findOrCreateByOpenId(String openId, String unionId, String nickname, String avatar) {
        UserSensitiveInfo sensitiveInfo = sensitiveInfoMapper.selectOne(
                new LambdaQueryWrapper<UserSensitiveInfo>()
                        .eq(UserSensitiveInfo::getOpenId, openId)
                        .last("limit 1"));
        if (sensitiveInfo != null) {
            return reuseExistingUser(sensitiveInfo, unionId);
        }

        String lockKey = "openId:" + openId;
        if (!authTokenSupport.tryAcquireNewUserLock(lockKey)) {
            throw new BusinessException("登录处理中，请稍后重试");
        }
        try {
            sensitiveInfo = sensitiveInfoMapper.selectOne(
                    new LambdaQueryWrapper<UserSensitiveInfo>()
                            .eq(UserSensitiveInfo::getOpenId, openId)
                            .last("limit 1"));
            if (sensitiveInfo != null) {
                return reuseExistingUser(sensitiveInfo, unionId);
            }

            log.info("[wx-login] 新用户注册 openId={}", openId);
            User user = createNewUser(
                    StringUtils.hasText(nickname) ? nickname : "微信用户" + randomSuffix(),
                    avatar,
                    3);
            UserSensitiveInfo info = new UserSensitiveInfo();
            info.setUserId(user.getId());
            info.setOpenId(openId);
            if (StringUtils.hasText(unionId)) {
                info.setUnionId(unionId);
            }
            info.setIsDelete(0);
            try {
                sensitiveInfoMapper.insert(info);
            } catch (DuplicateKeyException e) {
                sensitiveInfo = sensitiveInfoMapper.selectOne(
                        new LambdaQueryWrapper<UserSensitiveInfo>()
                                .eq(UserSensitiveInfo::getOpenId, openId)
                                .last("limit 1"));
                if (sensitiveInfo != null) {
                    return reuseExistingUser(sensitiveInfo, unionId);
                }
                throw e;
            }
            return user;
        } finally {
            authTokenSupport.releaseNewUserLock(lockKey);
        }
    }

    private User findOrCreateByPhone(String phone) {
        UserSensitiveInfo sensitiveInfo = sensitiveInfoMapper.selectOne(
                new LambdaQueryWrapper<UserSensitiveInfo>()
                        .eq(UserSensitiveInfo::getPhone, phone)
                        .last("limit 1"));
        if (sensitiveInfo != null) {
            return loadActiveUser(sensitiveInfo.getUserId());
        }

        String lockKey = "phone:" + phone;
        if (!authTokenSupport.tryAcquireNewUserLock(lockKey)) {
            throw new BusinessException("登录处理中，请稍后重试");
        }
        try {
            sensitiveInfo = sensitiveInfoMapper.selectOne(
                    new LambdaQueryWrapper<UserSensitiveInfo>()
                            .eq(UserSensitiveInfo::getPhone, phone)
                            .last("limit 1"));
            if (sensitiveInfo != null) {
                return loadActiveUser(sensitiveInfo.getUserId());
            }

            log.info("[phone-login] 新用户注册 phone={}", desensitizePhone(phone));
            User user = createNewUser("用户" + phone.substring(phone.length() - 4), null, 2);
            UserSensitiveInfo info = new UserSensitiveInfo();
            info.setUserId(user.getId());
            info.setPhone(phone);
            info.setIsDelete(0);
            try {
                sensitiveInfoMapper.insert(info);
            } catch (DuplicateKeyException e) {
                sensitiveInfo = sensitiveInfoMapper.selectOne(
                        new LambdaQueryWrapper<UserSensitiveInfo>()
                                .eq(UserSensitiveInfo::getPhone, phone)
                                .last("limit 1"));
                if (sensitiveInfo != null) {
                    return loadActiveUser(sensitiveInfo.getUserId());
                }
                throw e;
            }
            return user;
        } finally {
            authTokenSupport.releaseNewUserLock(lockKey);
        }
    }

    private User reuseExistingUser(UserSensitiveInfo sensitiveInfo, String unionId) {
        User user = loadActiveUser(sensitiveInfo.getUserId());
        if (StringUtils.hasText(unionId) && !StringUtils.hasText(sensitiveInfo.getUnionId())) {
            sensitiveInfo.setUnionId(unionId);
            sensitiveInfoMapper.updateById(sensitiveInfo);
        }
        return user;
    }

    private User loadActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete() == 1) {
            throw new BusinessException("账号异常，请联系客服");
        }
        return user;
    }

    private User createNewUser(String nickname, String avatar, Integer sourceType) {
        User user = new User();
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setStatus(1);
        user.setSourceType(sourceType);
        user.setRegisterTime(LocalDateTime.now());
        user.setLastActiveTime(LocalDateTime.now());
        user.setIsDelete(0);
        userMapper.insert(user);
        return user;
    }

    private String issueToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        authTokenSupport.storeToken(token, userId);
        return token;
    }

    private LoginResult toLoginResult(String token, User user) {
        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        return result;
    }

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String randomSuffix() {
        return Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000));
    }
}
