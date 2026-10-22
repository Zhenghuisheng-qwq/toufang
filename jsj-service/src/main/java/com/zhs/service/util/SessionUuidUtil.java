package com.zhs.service.util;

/**
 * 会话 ID 生成工具
 */
public final class SessionUuidUtil {

    private SessionUuidUtil() {
    }

    public static String generate(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        int compare = userId1.compareTo(userId2);
        if (compare < 0) {
            return userId1 + "_" + userId2;
        }
        if (compare > 0) {
            return userId2 + "_" + userId1;
        }
        throw new IllegalArgumentException("不能和自己聊天");
    }
}
