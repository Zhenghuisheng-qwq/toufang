package com.zhs.service.acq;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zhs.service.model.acq.AcqEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionSchemaTest {

    private static final Pattern TABLE = Pattern.compile(
            "CREATE TABLE IF NOT EXISTS `([a-z0-9_]+)` \\((.*?)\\) ENGINE", Pattern.DOTALL);
    private static final Pattern COLUMN = Pattern.compile("\\s*`([a-z0-9_]+)`\\s+[a-z]");
    private static final Set<String> BASE_COLUMNS = Set.of("id", "create_time", "update_time", "is_delete");

    @Test
    void sqlIsAdditiveAndMapsOntoEntities() throws Exception {
        String sql = Files.readString(sqlFile());
        String executable = sql.replaceAll("(?m)--.*$", "");
        String lower = executable.toLowerCase();
        assertFalse(lower.contains("drop database"));
        assertFalse(lower.contains("drop table"));
        assertFalse(lower.contains("truncate"));
        assertFalse(sql.contains("BEGIN PRIVATE"));
        assertFalse(sql.contains("BEGIN RSA"));
        assertTrue(sql.contains("api_v3_key_ref"));
        assertFalse(sql.contains("`api_v3_key`"));

        Matcher tables = TABLE.matcher(sql);
        List<String> names = new ArrayList<>();
        while (tables.find()) {
            names.add(tables.group(1));
            assertEntityFields(tables.group(1), tables.group(2));
        }
        assertEquals(27, names.size());
        assertTrue(names.containsAll(List.of(
                "landing_page", "landing_asset", "landing_domain", "site_domain", "domain_merchant",
                "report_template", "report_template_version", "trade_report",
                "pay_merchant", "pay_merchant_version", "trade_order", "pay_record", "pay_refund",
                "wecom_corp", "wecom_teacher", "wecom_route", "wecom_contact_entry", "wecom_tag",
                "wecom_external_contact", "wecom_customer_bind", "wecom_contact_event",
                "ad_account", "ad_click", "ad_conversion", "h5_visit", "h5_lead", "post_pay_task")));
        assertTrue(sql.contains("UNIQUE KEY `uk_order_no`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_success_order_no`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_order_platform_event`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_order_task`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_corp_external`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_state_token`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_link_id`"));
        assertTrue(sql.contains("UNIQUE KEY `uk_domain_merchant`"));
        assertTrue(sql.contains("`lease_owner`"));
        assertTrue(sql.contains("`lease_until`"));
        assertTrue(sql.contains("1=联系我二维码 2=获客助手链接"));
        assertFalse(sql.contains("雪花"));
        assertEquals(27, sql.split("AUTO_INCREMENT", -1).length - 1);
        TableId tableId = AcqEntity.class.getDeclaredField("id").getAnnotation(TableId.class);
        assertEquals(IdType.AUTO, tableId.type());
        for (String name : names) {
            Class<?> type = Class.forName("com.zhs.service.model.acq." + className(name));
            for (Field field : type.getDeclaredFields()) {
                assertFalse(field.isAnnotationPresent(TableId.class), name + " 不应重复声明主键");
            }
        }
    }

    private static void assertEntityFields(String table, String body) throws Exception {
        Class<?> type = Class.forName("com.zhs.service.model.acq." + className(table));
        Set<String> fields = new HashSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field.getName());
            }
        }
        Matcher columns = COLUMN.matcher(body);
        Set<String> mappedColumns = new HashSet<>();
        while (columns.find()) {
            String column = columns.group(1);
            if (column.startsWith("uk_") || column.startsWith("idx_")) {
                continue;
            }
            assertTrue(fields.contains(camel(column)), table + "." + column);
            mappedColumns.add(camel(column));
        }
        assertTrue(fields.contains("id"));
        for (Field field : type.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                assertTrue(mappedColumns.contains(field.getName()), table + " missing SQL column for " + field.getName());
            }
        }
        assertTrue(fields.contains("isDelete"));
        for (String base : BASE_COLUMNS) {
            assertTrue(fields.contains(camel(base)), table + " missing " + base);
        }
    }

    private static Path sqlFile() {
        Path direct = Path.of("sql", "acquisition.sql");
        if (Files.exists(direct)) {
            return direct;
        }
        return Path.of("..", "sql", "acquisition.sql");
    }

    private static String className(String table) {
        StringBuilder builder = new StringBuilder();
        for (String part : table.split("_")) {
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static String camel(String column) {
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < column.length(); i++) {
            char ch = column.charAt(i);
            if (ch == '_') {
                upper = true;
            } else if (upper) {
                builder.append(Character.toUpperCase(ch));
                upper = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
