package com.zhs.service.acq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 只连接调用方显式给出的隔离库。未提供地址时跳过，不读取 application-common.yml。
 */
class AcquisitionConstraintJdbcTest {

    @Test
    void uniqueConstraintsRejectDuplicates() throws Exception {
        String url = firstNonBlank(System.getProperty("acq.jdbc.url"), System.getenv("ACQ_JDBC_URL"));
        Assumptions.assumeTrue(url != null && !url.isBlank());
        String user = firstNonBlank(System.getProperty("acq.jdbc.user"), System.getenv("ACQ_JDBC_USER"));
        String password = firstNonBlank(System.getProperty("acq.jdbc.password"), System.getenv("ACQ_JDBC_PASSWORD"));
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            assertEquals("jsj_toufang_r0a", connection.getCatalog());
            assertEquals(27, countTables(connection));
            cleanup(connection);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO trade_order
                        (id, order_no, amount_fen, trade_type, pay_status, pay_merchant_id, merchant_version_id,
                         merchant_version_no, mch_id, app_id, domain_id, domain_name, corp_id, teacher_userid)
                        VALUES
                        (9100000000001, 'R0A-ORDER-1', 100, 1, 0, 1, 1, 1, 'mch-a', 'app-a', 1, 'a.example', 'corp-a', 'teacher-a')
                        """);
                assertDuplicate(statement, """
                        INSERT INTO trade_order
                        (id, order_no, amount_fen, trade_type, pay_status, pay_merchant_id, merchant_version_id,
                         merchant_version_no, mch_id, app_id)
                        VALUES
                        (9100000000002, 'R0A-ORDER-1', 100, 1, 0, 2, 2, 1, 'mch-b', 'app-b')
                        """);

                statement.executeUpdate("""
                        INSERT INTO post_pay_task (id, order_no, task_type, status, attempt_count, next_run_at)
                        VALUES (9100000000003, 'R0A-ORDER-1', 1, 0, 0, '2026-10-21 00:00:00')
                        """);
                assertDuplicate(statement, """
                        INSERT INTO post_pay_task (id, order_no, task_type, status, attempt_count, next_run_at)
                        VALUES (9100000000004, 'R0A-ORDER-1', 1, 0, 0, '2026-10-21 00:00:00')
                        """);

                statement.executeUpdate("""
                        INSERT INTO ad_conversion (id, order_no, platform, event_type, status, retry_count)
                        VALUES (9100000000005, 'R0A-ORDER-1', 1, 1, 0, 0)
                        """);
                assertDuplicate(statement, """
                        INSERT INTO ad_conversion (id, order_no, platform, event_type, status, retry_count)
                        VALUES (9100000000006, 'R0A-ORDER-1', 1, 1, 0, 0)
                        """);
                statement.executeUpdate("""
                        INSERT INTO ad_conversion (id, order_no, platform, event_type, status, retry_count)
                        VALUES (9100000000007, 'R0A-ORDER-1', 2, 1, 0, 0)
                        """);

                statement.executeUpdate("""
                        INSERT INTO pay_record
                        (id, order_no, out_trade_no, success_order_no, mch_id, app_id, amount_fen, trade_type, pay_status)
                        VALUES (9100000000008, 'R0A-ORDER-1', 'R0A-ORDER-1', 'R0A-ORDER-1', 'mch-a', 'app-a', 100, 1, 1)
                        """);
                assertDuplicate(statement, """
                        INSERT INTO pay_record
                        (id, order_no, out_trade_no, success_order_no, mch_id, app_id, amount_fen, trade_type, pay_status)
                        VALUES (9100000000009, 'R0A-ORDER-2', 'R0A-ORDER-2', 'R0A-ORDER-1', 'mch-a', 'app-a', 100, 1, 1)
                        """);

                statement.executeUpdate("""
                        INSERT INTO wecom_external_contact (id, corp_id, external_userid, status)
                        VALUES (9100000000010, 'r0a-corp-a', 'ext-1', 1)
                        """);
                assertDuplicate(statement, """
                        INSERT INTO wecom_external_contact (id, corp_id, external_userid, status)
                        VALUES (9100000000011, 'r0a-corp-a', 'ext-1', 1)
                        """);
                statement.executeUpdate("""
                        INSERT INTO wecom_external_contact (id, corp_id, external_userid, status)
                        VALUES (9100000000012, 'r0a-corp-b', 'ext-1', 1)
                        """);

                statement.executeUpdate("""
                        INSERT INTO wecom_contact_entry
                        (id, entry_type, order_no, corp_row_id, corp_id, teacher_userid, state_token, status)
                        VALUES (9100000000013, 1, 'R0A-ORDER-1', 1, 'r0a-corp-a', 'teacher-a', 'state-r0a-1', 1)
                        """);
                statement.executeUpdate("""
                        INSERT INTO wecom_contact_entry
                        (id, entry_type, order_no, corp_row_id, corp_id, teacher_userid, link_id, customer_channel, status)
                        VALUES (9100000000014, 2, 'R0A-ORDER-3', 1, 'r0a-corp-a', 'teacher-a', 'link-r0a-1', 'channel-r0a', 1)
                        """);
                assertDuplicate(statement, """
                        INSERT INTO wecom_contact_entry
                        (id, entry_type, order_no, corp_row_id, corp_id, teacher_userid, state_token, status)
                        VALUES (9100000000015, 1, 'R0A-ORDER-4', 1, 'r0a-corp-a', 'teacher-a', 'state-r0a-1', 1)
                        """);

                statement.executeUpdate("""
                        INSERT INTO wecom_route
                        (id, scope_type, corp_row_id, teacher_row_id, corp_id, teacher_userid, status, active_key, sort_no)
                        VALUES (9100000000016, 1, 1, 1, 'r0a-corp-a', 'teacher-a', 1, 'G', 0)
                        """);
                assertDuplicate(statement, """
                        INSERT INTO wecom_route
                        (id, scope_type, corp_row_id, teacher_row_id, corp_id, teacher_userid, status, active_key, sort_no)
                        VALUES (9100000000017, 1, 1, 2, 'r0a-corp-a', 'teacher-b', 1, 'G', 0)
                        """);
            } finally {
                cleanup(connection);
            }
        }
    }

    private static int countTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT table_name FROM information_schema.tables
                     WHERE table_schema = 'jsj_toufang_r0a'
                     """)) {
            Set<String> names = new HashSet<>();
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
            return names.size();
        }
    }

    private static void assertDuplicate(Statement statement, String sql) {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            assertEquals(1062, exception.getErrorCode(), exception.getMessage());
            return;
        }
        throw new AssertionError("重复数据没有被唯一约束拒绝");
    }

    private static void cleanup(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM wecom_route WHERE id >= 9100000000000 AND id < 9200000000000");
            statement.executeUpdate("DELETE FROM wecom_contact_entry WHERE order_no LIKE 'R0A-%'");
            statement.executeUpdate("DELETE FROM wecom_external_contact WHERE corp_id LIKE 'r0a-%'");
            statement.executeUpdate("DELETE FROM pay_record WHERE order_no LIKE 'R0A-%'");
            statement.executeUpdate("DELETE FROM ad_conversion WHERE order_no LIKE 'R0A-%'");
            statement.executeUpdate("DELETE FROM post_pay_task WHERE order_no LIKE 'R0A-%'");
            statement.executeUpdate("DELETE FROM trade_order WHERE order_no LIKE 'R0A-%'");
        }
    }

    @Test
    void autoIncrementFillsParentAndChildIds() throws Exception {
        String url = firstNonBlank(System.getProperty("acq.jdbc.url"), System.getenv("ACQ_JDBC_URL"));
        Assumptions.assumeTrue(url != null && !url.isBlank());
        String user = firstNonBlank(System.getProperty("acq.jdbc.user"), System.getenv("ACQ_JDBC_USER"));
        String password = firstNonBlank(System.getProperty("acq.jdbc.password"), System.getenv("ACQ_JDBC_PASSWORD"));
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            assertEquals("jsj_toufang_r0a", connection.getCatalog());
            assertEquals(27, countAutoIncrement(connection));
            long firstId = insertPage(connection, "R0B1-AUTO-A");
            long secondId = insertPage(connection, "R0B1-AUTO-B");
            assertTrue(firstId > 0);
            assertTrue(secondId > firstId);
            long assetId;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO landing_asset (landing_page_id, asset_type, asset_url, sort_no, status) VALUES (?, 1, ?, 0, 1)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, firstId);
                statement.setString(2, "/profile/upload/r0b1/a.png");
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    assetId = keys.getLong(1);
                }
            }
            assertTrue(assetId > 0);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT landing_page_id FROM landing_asset WHERE id = ?")) {
                statement.setLong(1, assetId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(firstId, resultSet.getLong(1));
                }
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM landing_asset WHERE id = " + assetId);
                statement.executeUpdate("DELETE FROM landing_page WHERE id IN (" + firstId + "," + secondId + ")");
            }
        }
    }

    private static int countAutoIncrement(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'jsj_toufang_r0a'
                        AND column_name = 'id'
                        AND extra LIKE '%auto_increment%'
                     """)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private static long insertPage(Connection connection, String title) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO landing_page (title, button_text, price_fen, publish_status)
                VALUES (?, '立即测试', 100, 0)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    @Test
    void scriptFileExistsForTheIsolatedDatabase() {
        assertTrue(Files.exists(Path.of("..", "sql", "acquisition.sql")) || Files.exists(Path.of("sql", "acquisition.sql")));
        assertFalse(firstNonBlank(System.getProperty("acq.jdbc.url"), System.getenv("ACQ_JDBC_URL")).contains("pupu"));
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
