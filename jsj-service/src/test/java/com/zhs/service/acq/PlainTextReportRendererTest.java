package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.report.PlainTextReportRenderer;
import com.zhs.service.acq.report.ReportInput;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class PlainTextReportRendererTest {
    private final PlainTextReportRenderer renderer = new PlainTextReportRenderer();

    @Test
    void substitutesOnlyProvidedFactsAndPreservesParagraphs() {
        ReportInput input = new ReportInput("测试姓名", 2, LocalDate.of(2000, 1, 2), null);
        assertEquals("测试姓名 女\n2000-01-02\n未提供 否", renderer.render(1,
                "{{name}} {{gender}}\n{{birthDate}}\n{{birthTime}} {{birthTimeProvided}}", input));
        assertNull(input.birthTime());
        assertEquals("00:00:00 是", renderer.render(1, "{{birthTime}} {{birthTimeProvided}}",
                new ReportInput(null, null, null, LocalTime.MIDNIGHT)));
    }

    @Test
    void missingInputIsDifferentFromAnInputWithOptionalFieldsMissing() {
        assertThrows(BusinessException.class, () -> renderer.render(1, "正文", null));
        assertEquals("未提供 未提供 未提供 未提供 否", renderer.render(1,
                "{{name}} {{gender}} {{birthDate}} {{birthTime}} {{birthTimeProvided}}",
                new ReportInput(null, null, null, null)));
    }

    @Test
    void rejectsUnknownOrMalformedVariablesWithoutEchoingContent() {
        for (String body : new String[]{"{{secret}}", "{{name", "name}}", "{{{{name}}}}", "{{T(java.lang.Runtime)}}"}) {
            BusinessException error = assertThrows(BusinessException.class, () -> renderer.validate(1, body));
            assertFalse(error.getMessage().contains(body));
        }
    }

    @Test
    void userTextIsNotReparsedOrExecuted() {
        String name = "{{birthDate}}<script>alert(1)</script>";
        assertEquals(name, renderer.render(1, "{{name}}", new ReportInput(name, 0, null, null)));
        assertEquals("${7*7}", renderer.render(1, "${7*7}", new ReportInput(null, null, null, null)));
        assertFalse(new ReportInput("私人姓名", 1, null, null).toString().contains("私人姓名"));
    }

    @Test
    void rejectsUnsupportedTypesAndBoundedInputs() {
        assertThrows(BusinessException.class, () -> renderer.validate(2, "正文"));
        assertThrows(BusinessException.class, () -> renderer.validate(1, "a".repeat(20001)));
        assertThrows(BusinessException.class, () -> renderer.validate(1, "{{name}}".repeat(257)));
        assertThrows(BusinessException.class, () -> renderer.render(1, "正文", new ReportInput("a".repeat(65), 0, null, null)));
        assertThrows(BusinessException.class, () -> renderer.render(1, "正文", new ReportInput("a\n", 0, null, null)));
        assertThrows(BusinessException.class, () -> renderer.render(1, "正文", new ReportInput(null, 3, null, null)));
    }
}
