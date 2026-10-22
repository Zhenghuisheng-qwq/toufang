package com.zhs.service.acq.report;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcquisitionCodes;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/** Bounded, single-pass text substitution; never evaluates expressions or reparses input. */
@Component
public final class PlainTextReportRenderer {
    public static final int MAX_TEMPLATE_LENGTH = 20_000;
    public static final int MAX_OUTPUT_LENGTH = 40_000;
    private static final int MAX_VARIABLES = 256;
    private static final Set<String> VARIABLES = Set.of(
            "name", "gender", "birthDate", "birthTime", "birthTimeProvided");

    public void validate(Integer contentType, String template) {
        process(contentType, template, null);
    }

    public String render(Integer contentType, String template, ReportInput input) {
        if (input == null) {
            throw new BusinessException("报告输入缺失");
        }
        String name = input.name();
        if (name != null && (name.length() > 64 || name.codePoints().anyMatch(Character::isISOControl))) {
            throw new BusinessException("姓名长度或字符不符合要求");
        }
        if (input.gender() != null && (input.gender() < 0 || input.gender() > 2)) {
            throw new BusinessException("性别值不符合要求");
        }
        Map<String, String> values = Map.of(
                "name", name == null || name.isBlank() ? "未提供" : name,
                "gender", input.gender() == null || input.gender() == 0 ? "未提供" : input.gender() == 1 ? "男" : "女",
                "birthDate", input.birthDate() == null ? "未提供" : input.birthDate().toString(),
                "birthTime", input.birthTime() == null ? "未提供" : input.birthTime().format(DateTimeFormatter.ISO_LOCAL_TIME),
                "birthTimeProvided", input.birthTime() == null ? "否" : "是");
        return process(contentType, template, values);
    }

    private String process(Integer contentType, String template, Map<String, String> values) {
        if (!Integer.valueOf(AcquisitionCodes.CONTENT_TEXT).equals(contentType)) {
            throw new BusinessException("不支持的报告内容格式，仅支持纯文本");
        }
        if (template == null || template.isBlank() || template.length() > MAX_TEMPLATE_LENGTH) {
            throw new BusinessException("模板正文不能为空且不能超过20000字");
        }
        StringBuilder output = new StringBuilder();
        int position = 0;
        int variables = 0;
        while (position < template.length()) {
            int start = template.indexOf("{{", position);
            int strayEnd = template.indexOf("}}", position);
            if (strayEnd >= 0 && (start < 0 || strayEnd < start)) {
                throw new BusinessException("模板占位符括号不匹配");
            }
            if (start < 0) {
                append(output, template.substring(position));
                break;
            }
            append(output, template.substring(position, start));
            int end = template.indexOf("}}", start + 2);
            if (end < 0 || ++variables > MAX_VARIABLES) {
                throw new BusinessException("模板占位符未闭合或数量超过256个");
            }
            String key = template.substring(start + 2, end);
            if (!VARIABLES.contains(key)) {
                // Do not echo arbitrary template content or submitted personal data.
                throw new BusinessException("模板包含未知变量，仅支持name、gender、birthDate、birthTime、birthTimeProvided");
            }
            append(output, values == null ? "" : values.get(key));
            position = end + 2;
        }
        return output.toString();
    }

    private void append(StringBuilder output, String value) {
        if (output.length() + value.length() > MAX_OUTPUT_LENGTH) {
            throw new BusinessException("报告正文超过40000字");
        }
        output.append(value);
    }
}
