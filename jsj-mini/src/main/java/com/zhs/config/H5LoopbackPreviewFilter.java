package com.zhs.config;

import com.zhs.service.acq.flow.H5LoopbackPreview;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 本地配置打开后，只给本机回环上的 H5 请求打预览标记。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class H5LoopbackPreviewFilter extends OncePerRequestFilter {

    @Value("${acq.h5.loopback-preview:false}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/h5")) {
                H5LoopbackPreview.openIf(enabled, request.getRemoteAddr(), request.getServerName());
            }
            chain.doFilter(request, response);
        } finally {
            H5LoopbackPreview.close();
        }
    }
}
