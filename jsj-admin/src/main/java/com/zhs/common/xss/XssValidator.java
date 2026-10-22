package com.zhs.common.xss;

import com.zhs.common.utils.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jsoup.safety.Safelist;
import org.jsoup.Jsoup;

/**
 * XSS 校验 —— 基于 Jsoup allowlist
 * <p>
 * 之前的硬编码正则 {@code <(\S*?)[^>]*>.*?|<.*? />} 可被 {@code <img onerror=>}、
 * {@code <svg/onload=>}、{@code javascript:} URL 等经典载荷绕过。
 * <p>
 * 现改为调用 {@link Jsoup#isValid(String, Safelist)}：默认 {@link Safelist#none()}，
 * 即任何 HTML 标签都视为非法；如需允许富文本字段，改用 {@link Safelist#basic()}
 * 或自定义 allowlist。
 */
public class XssValidator implements ConstraintValidator<Xss, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        // Jsoup.isValid 会尝试清洗并对比，任何标签/脚本残留都会返回 false
        return Jsoup.isValid(value, Safelist.none());
    }
}
