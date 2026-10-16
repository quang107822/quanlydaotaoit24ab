package com.example.quanlydaotao.n1;

import com.example.quanlydaotao.n1.dto.AssignRolesRequest;
import com.example.quanlydaotao.n1.dto.UserRoleRequest;
import com.example.quanlydaotao.n1.dto.UserRoleResponse;
import com.example.quanlydaotao.n1.entity.Role;
import com.example.quanlydaotao.n1.entity.User;
import com.example.quanlydaotao.n1.entity.UserRole;
import com.example.quanlydaotao.n1.repository.RoleRepository;
import com.example.quanlydaotao.n1.repository.UserRepository;
import com.example.quanlydaotao.n1.repository.UserRoleRepository;
import com.example.quanlydaotao.n1.service.UserRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm tra nghiệp vụ UserRoleService - gán / gỡ role cho user (không cần database). */
@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    // ==================== ASSIGN ====================

    @Test
    @DisplayName("Gán 1 role cho user: tạo dòng user_roles mới, mặc định active")
    void assignRoleCreatesAssignment() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.of(role(roleId, "ADMIN")));
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)).thenReturn(false);
        when(userRoleRepository.findFirstByUserIdAndRoleIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId, roleId))
                .thenReturn(Optional.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleResponse response = userRoleService.assignRole(request);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(captor.capture());

        UserRole saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRoleId()).isEqualTo(roleId);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRoleId()).isEqualTo(roleId);
    }

    @Test
    @DisplayName("Gán role cho user không tồn tại => 404, không ghi dữ liệu")
    void assignRoleWithUnknownUserReturns404() {

        UUID userId = UUID.randomUUID();

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleId(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.assignRole(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy user");

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Gán role không tồn tại => 404")
    void assignRoleWithUnknownRoleReturns404() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.assignRole(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy role");
    }

    @Test
    @DisplayName("Gán role mà user đã có => 409 (chống trùng vì bảng không có unique)")
    void assignRoleDuplicateReturns409() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.of(role(roleId, "ADMIN")));
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)).thenReturn(true);

        assertThatThrownBy(() -> userRoleService.assignRole(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("đã có role");

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    @DisplayName("Gán lại role từng bị gỡ: khôi phục dòng cũ thay vì tạo dòng mới")
    void assignRoleRestoresSoftDeletedAssignment() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID oldAssignmentId = UUID.randomUUID();

        UserRole softDeleted = new UserRole();
        softDeleted.setId(oldAssignmentId);
        softDeleted.setUserId(userId);
        softDeleted.setRoleId(roleId);
        softDeleted.setIsActive(false);
        softDeleted.setDeletedAt(LocalDateTime.now().minusDays(1));

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleId(roleId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.of(role(roleId, "ADMIN")));
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)).thenReturn(false);
        when(userRoleRepository.findFirstByUserIdAndRoleIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId, roleId))
                .thenReturn(Optional.of(softDeleted));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleResponse response = userRoleService.assignRole(request);

        assertThat(response.getId()).isEqualTo(oldAssignmentId);
        assertThat(softDeleted.getDeletedAt()).isNull();
        assertThat(softDeleted.getDeletedBy()).isNull();
        assertThat(softDeleted.getIsActive()).isTrue();
        assertThat(softDeleted.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Gán nhiều role: role đã có bị bỏ qua, role mới được thêm")
    void assignRolesSkipsExistingAndAddsNew() {

        UUID userId = UUID.randomUUID();
        UUID existingRoleId = UUID.randomUUID();
        UUID newRoleId = UUID.randomUUID();

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(existingRoleId, newRoleId, existingRoleId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(existingRoleId))
                .thenReturn(Optional.of(role(existingRoleId, "ADMIN")));
        when(roleRepository.findByIdAndDeletedAtIsNull(newRoleId))
                .thenReturn(Optional.of(role(newRoleId, "LECTURER")));
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, existingRoleId)).thenReturn(true);
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, newRoleId)).thenReturn(false);
        when(userRoleRepository.findFirstByUserIdAndRoleIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId, newRoleId))
                .thenReturn(Optional.empty());
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        userRoleService.assignRoles(userId, request);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        // roleId trùng lặp trong request chỉ được xử lý 1 lần => chỉ có 1 dòng mới được lưu
        verify(userRoleRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getRoleId()).isEqualTo(newRoleId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Gán nhiều role với danh sách rỗng => 400")
    void assignRolesWithEmptyListReturns400() {

        UUID userId = UUID.randomUUID();

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        assertThatThrownBy(() -> userRoleService.assignRoles(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("roleIds");
    }

    // ==================== SYNC ====================

    @Test
    @DisplayName("Đồng bộ role: role không còn trong danh sách bị gỡ (xoá mềm)")
    void syncRolesRemovesRolesNotInList() {

        UUID userId = UUID.randomUUID();
        UUID removedRoleId = UUID.randomUUID();
        UUID keptRoleId = UUID.randomUUID();

        UserRole removed = assignment(UUID.randomUUID(), userId, removedRoleId);
        UserRole kept = assignment(UUID.randomUUID(), userId, keptRoleId);

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(keptRoleId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(keptRoleId))
                .thenReturn(Optional.of(role(keptRoleId, "LECTURER")));
        when(userRoleRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(removed, kept));
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, keptRoleId)).thenReturn(true);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        userRoleService.syncRoles(userId, request);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue().getId()).isEqualTo(removed.getId());
        assertThat(removed.getDeletedAt()).isNotNull();
        assertThat(removed.getIsActive()).isFalse();

        // role vẫn nằm trong danh sách thì không bị đụng tới
        assertThat(kept.getDeletedAt()).isNull();
        assertThat(kept.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Đồng bộ role với roleIds = null => 400")
    void syncRolesWithoutRoleIdsReturns400() {

        UUID userId = UUID.randomUUID();
        AssignRolesRequest request = new AssignRolesRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));

        assertThatThrownBy(() -> userRoleService.syncRoles(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("roleIds");
    }

    // ==================== REVOKE / RESTORE ====================

    @Test
    @DisplayName("Gỡ 1 role cụ thể khỏi user: xoá mềm và tắt active")
    void revokeRoleSoftDeletes() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UserRole existing = assignment(UUID.randomUUID(), userId, roleId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.of(role(roleId, "ADMIN")));
        when(userRoleRepository.findFirstByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId))
                .thenReturn(Optional.of(existing));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        userRoleService.revokeRole(userId, roleId);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Gỡ role mà user không có => 404")
    void revokeRoleNotAssignedReturns404() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.of(role(roleId, "ADMIN")));
        when(userRoleRepository.findFirstByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.revokeRole(userId, roleId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("không có role");
    }

    @Test
    @DisplayName("Khôi phục phân quyền đã gỡ: bỏ deletedAt và bật lại active")
    void restoreAssignmentClearsDeletedAt() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        UserRole softDeleted = assignment(assignmentId, userId, roleId);
        softDeleted.setDeletedAt(LocalDateTime.now().minusHours(1));
        softDeleted.setIsActive(false);

        when(userRoleRepository.findById(assignmentId)).thenReturn(Optional.of(softDeleted));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(roleRepository.findByIdAndDeletedAtIsNull(roleId)).thenReturn(Optional.of(role(roleId, "ADMIN")));
        when(userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)).thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleResponse response = userRoleService.restore(assignmentId);

        assertThat(response.getId()).isEqualTo(assignmentId);
        assertThat(softDeleted.getDeletedAt()).isNull();
        assertThat(softDeleted.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Khôi phục phân quyền chưa bị gỡ => 409")
    void restoreActiveAssignmentReturns409() {

        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        UserRole active = assignment(assignmentId, userId, roleId);

        when(userRoleRepository.findById(assignmentId)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> userRoleService.restore(assignmentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("không cần khôi phục");
    }

    // ==================== HELPERS ====================

    private static User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setUsername("sv01");
        user.setFullName("Nguyễn Văn A");
        user.setIsActive(true);
        return user;
    }

    private static Role role(UUID id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName("Vai trò " + code);
        role.setIsActive(true);
        return role;
    }

    private static UserRole assignment(UUID id, UUID userId, UUID roleId) {
        UserRole userRole = new UserRole();
        userRole.setId(id);
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setIsActive(true);
        userRole.setCreatedAt(LocalDateTime.now().minusDays(1));
        return userRole;
    }
}
