package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;

/**
 * 素材只接受后台上传目录中的图片地址，不接收任意外链。
 */
public final class AssetUrls {

    private AssetUrls() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("图片地址不能为空");
        }
        String value = raw.trim();
        if (!value.startsWith("/profile/upload/")) {
            throw new BusinessException("图片必须来自后台上传目录");
        }
        if (value.contains("..") || value.contains("\\") || value.contains("://") || value.contains("?")) {
            throw new BusinessException("图片地址不合法");
        }
        String lower = value.toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".bmp"))) {
            throw new BusinessException("只支持 png、jpg、jpeg、gif、bmp 图片");
        }
        if (value.length() > 512) {
            throw new BusinessException("图片地址过长");
        }
        return value;
    }
}
