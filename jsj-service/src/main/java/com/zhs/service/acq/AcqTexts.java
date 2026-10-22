package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;

/**
 * 后台配置输入的长度、开关和分页边界。
 */
public final class AcqTexts {

    private AcqTexts() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String requireText(String value, String label, int max) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(label + "不能为空");
        }
        if (trimmed.length() > max) {
            throw new BusinessException(label + "不能超过" + max + "个字符");
        }
        return trimmed;
    }

    public static String optionalText(String value, String label, int max) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.length() > max) {
            throw new BusinessException(label + "不能超过" + max + "个字符");
        }
        return trimmed;
    }

    public static int requireFlag(Integer value, String label) {
        if (value == null || (value != AcquisitionCodes.SWITCH_OFF && value != AcquisitionCodes.SWITCH_ON)) {
            throw new BusinessException(label + "只能是开启或关闭");
        }
        return value;
    }

    public static int requireChoice(Integer value, String label, int... allowed) {
        if (value == null) {
            throw new BusinessException(label + "不能为空");
        }
        for (int item : allowed) {
            if (item == value) {
                return value;
            }
        }
        throw new BusinessException(label + "取值不正确");
    }

    public static String likePattern(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        String escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public static long pageNum(Long value) {
        if (value == null || value < 1) {
            return 1L;
        }
        return value;
    }

    public static long pageSize(Long value) {
        if (value == null || value < 1) {
            return 10L;
        }
        return Math.min(value, 100L);
    }
}
