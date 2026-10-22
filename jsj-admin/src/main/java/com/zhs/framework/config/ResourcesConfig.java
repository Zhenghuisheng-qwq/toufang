package com.zhs.framework.config;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.zhs.common.config.RuoYiConfig;
import com.zhs.common.constant.Constants;
import com.zhs.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 通用配置
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    /**
     * 允许的 CORS 源列表，逗号分隔。
     * 默认本地联调域名；生产通过环境变量 CORS_ALLOWED_ORIGINS 注入真实域名，例如：
     *   CORS_ALLOWED_ORIGINS=https://admin.example.com,https://h5.example.com
     *
     * 单值 "*" 将退化为放行全部（仅建议内网），会自动关闭 allowCredentials。
     */
    @Value("${cors.allowed-origins:http://localhost,http://localhost:80,http://localhost:5173,http://localhost:8080,http://127.0.0.1,http://127.0.0.1:80,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 */
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");

        /** swagger配置 */
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    /**
     * 跨域配置 —— 白名单化，避免通配 `*` 暴露后台接口给任意域。
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
        } else {
            // addAllowedOriginPattern 支持通配，避免浏览器省略默认端口导致严格匹配失败
            // 例如 http://localhost:80 的页面发出的 Origin 是 http://localhost
            origins.forEach(config::addAllowedOriginPattern);
            config.setAllowCredentials(true);
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
