package com.example.quanlydaotao.n1;

import com.example.quanlydaotao.n1.entity.Permission;
import com.example.quanlydaotao.n1.entity.Role;
import com.example.quanlydaotao.n1.entity.User;
import com.example.quanlydaotao.n1.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra mapping của các entity nhóm 1 có khớp đúng cột trong database/it24ab.sql hay không.
 *
 * Test này sinh ra để chặn lỗi cũ: entity Permission có cột "action" không tồn tại trong DB
 * (mọi truy vấn permissions đều lỗi "Invalid column name 'action'") và thiếu cột "code".
 *
 * Test đọc trực tiếp file SQL nên không cần kết nối database.
 */
class EntitySchemaMappingTest {

    private static final Path SQL_DUMP = Paths.get("database", "it24ab.sql");

    private static final Pattern COLUMN_LINE = Pattern.compile("\\[(\\w+)\\]\\s+\\[");

    @Test
    @DisplayName("Bảng permissions khớp entity Permission")
    void permissionsTableMatchesEntity() throws Exception {
        assertMapping(Permission.class, "permissions");
    }

    @Test
    @DisplayName("Bảng user_roles khớp entity UserRole")
    void userRolesTableMatchesEntity() throws Exception {
        assertMapping(UserRole.class, "user_roles");
    }

    @Test
    @DisplayName("Bảng roles khớp entity Role")
    void rolesTableMatchesEntity() throws Exception {
        assertMapping(Role.class, "roles");
    }

    @Test
    @DisplayName("Bảng users khớp entity User")
    void usersTableMatchesEntity() throws Exception {
        assertMapping(User.class, "users");
    }

    // ==================== HELPERS ====================

    private void assertMapping(Class<?> entityClass, String tableName) throws Exception {

        Set<String> entityColumns = entityColumns(entityClass);
        Set<String> dbColumns = dbColumns(tableName);

        assertThat(entityColumns)
                .as("Cột của entity %s phải khớp bảng [%s] trong %s. "
                                + "Cột thừa trong entity: %s. Cột thiếu trong entity: %s",
                        entityClass.getSimpleName(), tableName, SQL_DUMP,
                        difference(entityColumns, dbColumns), difference(dbColumns, entityColumns))
                .containsExactlyInAnyOrderElementsOf(dbColumns);
    }

    /** Lấy danh sách tên cột từ các annotation @Column của entity. */
    private static Set<String> entityColumns(Class<?> entityClass) {

        Entity entity = entityClass.getAnnotation(Entity.class);
        Table table = entityClass.getAnnotation(Table.class);

        assertThat(entity).as("%s phải là @Entity", entityClass.getSimpleName()).isNotNull();
        assertThat(table).as("%s phải có @Table", entityClass.getSimpleName()).isNotNull();

        Set<String> columns = new LinkedHashSet<>();

        for (Field field : entityClass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                columns.add(column.name());
            }
        }

        return columns;
    }

    /** Lấy danh sách cột của 1 bảng từ file dump SQL (UTF-16LE). */
    private static Set<String> dbColumns(String tableName) throws IOException {

        assertThat(Files.exists(SQL_DUMP))
                .as("Không tìm thấy file %s (test phải chạy với working directory là thư mục project)",
                        SQL_DUMP.toAbsolutePath())
                .isTrue();

        String sql = Files.readString(SQL_DUMP, StandardCharsets.UTF_16LE);

        // Bảng "students_", "students" ... có tiền tố giống nhau nên phải khớp chính xác tên bảng
        String createTable = "CREATE TABLE [dbo].[" + tableName + "](";

        String[] lines = sql.split("\\r?\\n");

        int startIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(createTable)) {
                startIndex = i;
                break;
            }
        }

        assertThat(startIndex)
                .as("Không tìm thấy %s trong %s", createTable, SQL_DUMP)
                .isGreaterThanOrEqualTo(0);

        // Mỗi dòng định nghĩa cột bắt đầu bằng tab + [tên_cột] [kiểu_dữ_liệu]
        Set<String> columns = new LinkedHashSet<>();

        for (int i = startIndex + 1; i < lines.length; i++) {

            String line = lines[i];

            if (!line.startsWith("\t")) {
                break;
            }

            Matcher matcher = COLUMN_LINE.matcher(line);
            if (!matcher.find()) {
                break;
            }

            columns.add(matcher.group(1));
        }

        assertThat(columns).as("Bảng [%s] phải có ít nhất 1 cột", tableName).isNotEmpty();

        return columns;
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> diff = new LinkedHashSet<>(left);
        diff.removeAll(right);
        return diff;
    }
}
