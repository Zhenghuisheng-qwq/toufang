package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 金额以分持久化。页面按元录入，转换只使用十进制，不使用浮点。
 */
public final class MoneyAmounts {

    public static final int MIN_FEN = 1;
    public static final int MAX_FEN = 99_999_999;

    private MoneyAmounts() {
    }

    public static int yuanToFen(String yuanText) {
        if (yuanText == null || yuanText.isBlank()) {
            throw new BusinessException("价格不能为空");
        }
        String text = yuanText.trim();
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new BusinessException("价格格式不正确，最多两位小数");
        }
        BigDecimal yuan = new BigDecimal(text);
        BigDecimal fen = yuan.movePointRight(2);
        int value;
        try {
            value = fen.intValueExact();
        } catch (ArithmeticException ex) {
            throw new BusinessException("价格超出允许范围");
        }
        if (value < MIN_FEN || value > MAX_FEN) {
            throw new BusinessException("价格必须大于0且不超过999999.99元");
        }
        return value;
    }

    public static String fenToYuan(Integer fen) {
        if (fen == null) {
            return "";
        }
        return BigDecimal.valueOf(fen.longValue()).movePointLeft(2).setScale(2).toPlainString();
    }
}
