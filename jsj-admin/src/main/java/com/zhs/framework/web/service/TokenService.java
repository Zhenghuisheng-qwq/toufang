package com.zhs.framework.web.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.zhs.common.constant.CacheConstants;
import com.zhs.common.constant.Constants;
import com.zhs.system.model.LoginUser;
import com.zhs.common.core.redis.RedisCache;
import com.zhs.common.utils.ServletUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.http.UserAgentUtils;
import com.zhs.common.utils.ip.AddressUtils;
import com.zhs.common.utils.ip.IpUtils;
import com.zhs.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * token验证处理
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    /** 缓存 SecretKey，避免每次请求都重新派生 */
    private SecretKey signingKey;

    @Autowired
    private RedisCache redisCache;

    /**
     * 初始化签名密钥：
     * HS512 要求 ≥64 字节。若配置的 secret 不够长，走 SHA-512 派生一个安全长度的密钥，
     * 避免启动即抛 WeakKeyException，同时提供足够熵。
     * 生产环境务必通过 JWT_SECRET 环境变量注入 ≥64 字节的随机字符串。
     */
    @PostConstruct
    private void initKey()
    {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("token.secret 未配置");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 64) {
            log.warn("JWT secret 长度不足 64 字节，已自动 SHA-512 派生；生产环境请通过环境变量 JWT_SECRET 注入强密钥");
            try {
                raw = MessageDigest.getInstance("SHA-512").digest(raw);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("派生 JWT 密钥失败", e);
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(raw);
    }

    public LoginUser getLoginUser(HttpServletRequest request)
    {
        String token = getToken(request);
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        try
        {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = getTokenKey(uuid);
            return redisCache.getCacheObject(userKey);
        }
        catch (ExpiredJwtException e)
        {
            log.warn("token 已过期");
        }
        catch (JwtException e)
        {
            log.warn("token 解析失败: {}", e.getMessage());
        }
        catch (Exception e)
        {
            log.error("获取用户信息异常", e);
        }
        return null;
    }

    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = Map.of(
                Constants.LOGIN_USER_KEY, token,
                Constants.JWT_USERNAME, loginUser.getUsername() == null ? "" : loginUser.getUsername());
        return createToken(claims);
    }

    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    /**
     * 从数据声明生成令牌 — 带 issuedAt + expiration，不再完全依赖 Redis 注销
     */
    private String createToken(Map<String, Object> claims)
    {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        // JWT 本体过期时间比 Redis 略长，保证 refreshToken 场景下旧 token 仍可验签
        Date expiresAt = new Date(now + (expireTime + 30L) * MILLIS_MINUTE);
        return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        // 用户名存在 JWT_USERNAME claim 里，不是 subject
        Object name = claims.get(Constants.JWT_USERNAME);
        return name == null ? null : name.toString();
    }

    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }
}
