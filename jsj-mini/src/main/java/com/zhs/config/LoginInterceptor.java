package com.zhs.config;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.common.cachekey.user.TokenCacheKey;
import com.zhs.service.auth.AuthService;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截：Redis 优先，回退走 jsj-service AuthService 查会话表。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private AuthService authService;

    @Resource
    private MiniRedisUtil miniRedisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        NoLoginRequest methodAnnotation = handlerMethod.getMethodAnnotation(NoLoginRequest.class);
        NoLoginRequest classAnnotation = handlerMethod.getBeanType().getAnnotation(NoLoginRequest.class);
        if (methodAnnotation != null || classAnnotation != null) {
            return true;
        }

        String token = TokenUtil.resolveFrom(request);
        if (token == null || token.isEmpty()) {
            sendUnauthorized(response, "未登录");
            return false;
        }

        TokenCacheKey cacheKey = new TokenCacheKey(token);
        Object userIdObj = miniRedisUtil.get(cacheKey);
        if (userIdObj != null) {
            UserContext.setUserId(Long.valueOf(userIdObj.toString()));
            return true;
        }

        Long userId = authService.findActiveUserIdByToken(token);
        if (userId == null) {
            sendUnauthorized(response, "登录已过期");
            return false;
        }

        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
