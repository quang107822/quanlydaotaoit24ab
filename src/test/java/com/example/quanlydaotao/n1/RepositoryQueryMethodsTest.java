package com.example.quanlydaotao.n1;

import com.example.quanlydaotao.n1.repository.PermissionRepository;
import com.example.quanlydaotao.n1.repository.RoleRepository;
import com.example.quanlydaotao.n1.repository.StudentRepository;
import com.example.quanlydaotao.n1.repository.UserRepository;
import com.example.quanlydaotao.n1.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Spring Data JPA validate tên method (findBy..., existsBy...) lúc khởi động ứng dụng.
 * Test này parse trước các method đó bằng PartTree để phát hiện sai tên thuộc tính
 * mà không cần kết nối database.
 */
class RepositoryQueryMethodsTest {

    @Test
    @DisplayName("PermissionRepository: tên method derived query hợp lệ")
    void permissionRepositoryMethodsAreValid() {
        assertDerivedQueriesAreValid(PermissionRepository.class);
    }

    @Test
    @DisplayName("UserRoleRepository: tên method derived query hợp lệ")
    void userRoleRepositoryMethodsAreValid() {
        assertDerivedQueriesAreValid(UserRoleRepository.class);
    }

    @Test
    @DisplayName("RoleRepository: tên method derived query hợp lệ")
    void roleRepositoryMethodsAreValid() {
        assertDerivedQueriesAreValid(RoleRepository.class);
    }

    @Test
    @DisplayName("UserRepository: tên method derived query hợp lệ")
    void userRepositoryMethodsAreValid() {
        assertDerivedQueriesAreValid(UserRepository.class);
    }

    @Test
    @DisplayName("StudentRepository: tên method derived query hợp lệ")
    void studentRepositoryMethodsAreValid() {
        assertDerivedQueriesAreValid(StudentRepository.class);
    }

    // ==================== HELPERS ====================

    private void assertDerivedQueriesAreValid(Class<?> repositoryInterface) {

        Class<?> domainType = ResolvableType
                .forClass(repositoryInterface)
                .as(JpaRepository.class)
                .getGeneric(0)
                .resolve();

        assertThatCode(() -> {
            if (domainType == null) {
                throw new IllegalStateException("Không xác định được entity của " + repositoryInterface);
            }
        }).doesNotThrowAnyException();

        for (Method method : repositoryInterface.getDeclaredMethods()) {

            // Bỏ qua method có @Query tự viết và method mặc định/static
            if (method.isAnnotationPresent(Query.class)
                    || method.isDefault()
                    || Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            assertThatCode(() -> new PartTree(method.getName(), domainType))
                    .as("Method %s#%s không phải derived query hợp lệ cho entity %s",
                            repositoryInterface.getSimpleName(), method.getName(),
                            domainType == null ? "?" : domainType.getSimpleName())
                    .doesNotThrowAnyException();
        }
    }
}
