package com.example.quanlydaotao.n1;

import com.example.quanlydaotao.n1.entity.Permission;
import com.example.quanlydaotao.n1.repository.PermissionRepository;
import com.example.quanlydaotao.n1.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm tra nghiệp vụ PermissionService (không cần database). */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    // ==================== CREATE ====================

    @Test
    @DisplayName("Tạo quyền: chuẩn hoá code, sinh id mới, mặc định active")
    void createPermissionNormalizesCodeAndGeneratesId() {

        UUID clientId = UUID.randomUUID();
        Permission request = new Permission();
        request.setId(clientId);                  // id client gửi lên phải bị bỏ qua
        request.setCode("  user_view  ");
        request.setName("  Xem người dùng  ");
        request.setModule("  users  ");

        when(permissionRepository.findByCodeIgnoreCase("USER_VIEW")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission saved = permissionService.createPermission(request);

        assertThat(saved.getId()).isNotEqualTo(clientId).isNotNull();
        assertThat(saved.getCode()).isEqualTo("USER_VIEW");
        assertThat(saved.getName()).isEqualTo("Xem người dùng");
        assertThat(saved.getModule()).isEqualTo("USERS");
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Tạo quyền thiếu code => 400, không lưu")
    void createPermissionWithoutCodeReturns400() {

        Permission request = new Permission();
        request.setName("Xem người dùng");

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("code");

        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("Tạo quyền trùng code => 409")
    void createPermissionWithDuplicateCodeReturns409() {

        Permission existing = permission(UUID.randomUUID(), "USER_VIEW", "Xem người dùng");

        Permission request = new Permission();
        request.setCode("USER_VIEW");
        request.setName("Xem người dùng khác");

        when(permissionRepository.findByCodeIgnoreCase("USER_VIEW")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("đã tồn tại");

        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("Tạo quyền trùng code với bản ghi đã xoá mềm => 409 kèm hướng dẫn restore")
    void createPermissionWithSoftDeletedCodeReturns409WithHint() {

        Permission deleted = permission(UUID.randomUUID(), "USER_VIEW", "Xem người dùng");
        deleted.setDeletedAt(LocalDateTime.now());

        Permission request = new Permission();
        request.setCode("USER_VIEW");
        request.setName("Xem người dùng");

        when(permissionRepository.findByCodeIgnoreCase("USER_VIEW")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("đã xoá mềm");
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("Cập nhật chỉ field được gửi lên, không làm mất description/module")
    void updatePermissionKeepsUntouchedFields() {

        UUID id = UUID.randomUUID();

        Permission existing = permission(id, "USER_VIEW", "Xem người dùng");
        existing.setModule("USERS");
        existing.setDescription("Mô tả cũ");

        Permission request = new Permission();
        request.setName("Xem người dùng (mới)");

        when(permissionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission updated = permissionService.updatePermission(id, request);

        assertThat(updated.getName()).isEqualTo("Xem người dùng (mới)");
        assertThat(updated.getCode()).isEqualTo("USER_VIEW");
        assertThat(updated.getModule()).isEqualTo("USERS");
        assertThat(updated.getDescription()).isEqualTo("Mô tả cũ");
    }

    @Test
    @DisplayName("Đổi code sang code đã bị bản ghi khác dùng => 409")
    void updatePermissionToExistingCodeReturns409() {

        UUID id = UUID.randomUUID();
        Permission existing = permission(id, "USER_VIEW", "Xem người dùng");

        Permission request = new Permission();
        request.setCode("USER_DELETE");

        when(permissionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
        when(permissionRepository.existsByCodeIgnoreCaseAndIdNot("USER_DELETE", id)).thenReturn(true);

        assertThatThrownBy(() -> permissionService.updatePermission(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("đã tồn tại");
    }

    // ==================== READ / DELETE ====================

    @Test
    @DisplayName("Lấy quyền đã xoá mềm => 404 (không trả về dữ liệu đã xoá)")
    void getPermissionByIdOfDeletedRecordReturns404() {

        UUID id = UUID.randomUUID();
        when(permissionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.getPermissionById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy permission");
    }

    @Test
    @DisplayName("Tìm theo tên trả về danh sách (name không unique trong DB)")
    void getByNameReturnsList() {

        Permission first = permission(UUID.randomUUID(), "USER_VIEW", "Xem người dùng");
        Permission second = permission(UUID.randomUUID(), "STUDENT_VIEW", "Xem người dùng");

        when(permissionRepository.findByNameIgnoreCaseAndDeletedAtIsNull("Xem người dùng"))
                .thenReturn(List.of(first, second));

        assertThat(permissionService.getByName("Xem người dùng")).hasSize(2);
    }

    @Test
    @DisplayName("Xoá mềm: set deletedAt và tắt active")
    void deletePermissionSoftDeletes() {

        UUID id = UUID.randomUUID();
        Permission existing = permission(id, "USER_VIEW", "Xem người dùng");
        existing.setIsActive(true);

        when(permissionRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        permissionService.deletePermission(id);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getIsActive()).isFalse();
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Khôi phục quyền đã xoá mềm: bỏ deletedAt và bật lại active")
    void restorePermissionClearsDeletedAt() {

        UUID id = UUID.randomUUID();
        Permission deleted = permission(id, "USER_VIEW", "Xem người dùng");
        deleted.setDeletedAt(LocalDateTime.now());
        deleted.setIsActive(false);

        when(permissionRepository.findById(id)).thenReturn(Optional.of(deleted));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission restored = permissionService.restorePermission(id);

        assertThat(restored.getDeletedAt()).isNull();
        assertThat(restored.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Tìm kiếm với keyword rỗng trả về toàn bộ danh sách chưa xoá")
    void searchWithBlankKeywordReturnsAll() {

        when(permissionRepository.findByDeletedAtIsNull())
                .thenReturn(List.of(permission(UUID.randomUUID(), "USER_VIEW", "Xem người dùng")));

        assertThat(permissionService.searchPermissions("  ")).hasSize(1);

        verify(permissionRepository, never()).searchActivePermissions(anyString());
    }

    // ==================== HELPERS ====================

    private static Permission permission(UUID id, String code, String name) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setName(name);
        permission.setIsActive(true);
        return permission;
    }
}
