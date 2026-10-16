package com.example.quanlydaotao.n1.service;

import com.example.quanlydaotao.n1.dto.AssignRolesRequest;
import com.example.quanlydaotao.n1.dto.UserRoleRequest;
import com.example.quanlydaotao.n1.dto.UserRoleResponse;
import com.example.quanlydaotao.n1.entity.Role;
import com.example.quanlydaotao.n1.entity.User;
import com.example.quanlydaotao.n1.entity.UserRole;
import com.example.quanlydaotao.n1.repository.RoleRepository;
import com.example.quanlydaotao.n1.repository.UserRepository;
import com.example.quanlydaotao.n1.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ gán vai trò cho người dùng (bảng trung gian user_roles).
 *
 * Mỗi user có thể có 1 hoặc nhiều role; mỗi role có thể thuộc nhiều user.
 * Bảng user_roles không có unique (user_id, role_id) nên service tự chống trùng,
 * và mọi thao tác gỡ role đều là xoá mềm (deleted_at) để giữ lịch sử.
 */
@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository,
                           UserRepository userRepository,
                           RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    // ==================== READ ====================

    /** Tất cả phân quyền chưa xoá mềm. */
    public List<UserRoleResponse> getAll() {
        return toResponses(userRoleRepository.findByDeletedAtIsNull());
    }

    /** Chi tiết 1 dòng phân quyền. */
    public UserRoleResponse getById(UUID id) {
        UserRole userRole = requireUserRole(id);
        return toResponses(List.of(userRole)).get(0);
    }

    /** Tất cả phân quyền của 1 user (gồm cả dòng đang bị tắt). */
    public List<UserRoleResponse> getByUserId(UUID userId) {
        requireUser(userId);
        return toResponses(userRoleRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId));
    }

    /** Tất cả phân quyền của 1 role (danh sách user đang giữ role đó). */
    public List<UserRoleResponse> getByRoleId(UUID roleId) {
        requireRole(roleId);
        return toResponses(userRoleRepository.findByRoleIdAndDeletedAtIsNullOrderByCreatedAtAsc(roleId));
    }

    /** Danh sách role đang có hiệu lực của user. */
    public List<Role> getRolesByUserId(UUID userId) {
        requireUser(userId);

        List<UserRole> assignments =
                userRoleRepository.findByUserIdAndIsActiveAndDeletedAtIsNull(userId, true);

        List<UUID> roleIds = assignments.stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (roleIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Role> roles = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        return assignments.stream()
                .map(assignment -> roles.get(assignment.getRoleId()))
                .filter(Objects::nonNull)
                .toList();
    }

    // ==================== WRITE ====================

    /** Gán 1 role cho 1 user. */
    @Transactional
    public UserRoleResponse assignRole(UserRoleRequest request) {

        if (request == null) {
            throw badRequest("Dữ liệu gán role không được để trống");
        }

        User user = requireUser(request.getUserId());
        Role role = requireRole(request.getRoleId());

        if (hasActiveAssignment(user.getId(), role.getId())) {
            throw conflict("User '" + user.getUsername() + "' đã có role '" + role.getCode() + "'");
        }

        UserRole saved = createOrRestoreAssignment(
                user.getId(), role.getId(), request.getCreatedBy(), request.getUpdatedBy());

        return toResponses(List.of(saved)).get(0);
    }

    /**
     * Gán thêm nhiều role cho 1 user (giữ nguyên các role đã có).
     * Role nào đã có thì bỏ qua, role nào từng bị gỡ thì khôi phục lại.
     */
    @Transactional
    public List<UserRoleResponse> assignRoles(UUID userId, AssignRolesRequest request) {

        requireUser(userId);

        Set<UUID> roleIds = distinctRoleIds(request == null ? null : request.getRoleIds(), false);

        for (UUID roleId : roleIds) {
            requireRole(roleId);
            if (!hasActiveAssignment(userId, roleId)) {
                createOrRestoreAssignment(userId, roleId,
                        request.getCreatedBy(), request.getUpdatedBy());
            }
        }

        return getByUserId(userId);
    }

    /**
     * Đồng bộ danh sách role của user: danh sách gửi lên là TOÀN BỘ role của user.
     * Role không nằm trong danh sách sẽ bị gỡ (xoá mềm), role trong danh sách sẽ được gán/khôi phục.
     * Gửi mảng rỗng nghĩa là gỡ hết role của user.
     */
    @Transactional
    public List<UserRoleResponse> syncRoles(UUID userId, AssignRolesRequest request) {

        requireUser(userId);

        if (request == null || request.getRoleIds() == null) {
            throw badRequest("Danh sách roleIds là bắt buộc (gửi mảng rỗng nếu muốn gỡ hết role)");
        }

        Set<UUID> targetRoleIds = distinctRoleIds(request.getRoleIds(), true);

        // Kiểm tra toàn bộ role hợp lệ trước khi thay đổi dữ liệu
        for (UUID roleId : targetRoleIds) {
            requireRole(roleId);
        }

        LocalDateTime now = LocalDateTime.now();

        // Gỡ các role không còn trong danh sách
        List<UserRole> current = userRoleRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId);
        for (UserRole assignment : current) {
            if (!targetRoleIds.contains(assignment.getRoleId())) {
                softDelete(assignment, now, request.getUpdatedBy());
            }
        }

        // Gán (hoặc khôi phục) các role trong danh sách
        for (UUID roleId : targetRoleIds) {
            if (!hasActiveAssignment(userId, roleId)) {
                createOrRestoreAssignment(userId, roleId,
                        request.getCreatedBy(), request.getUpdatedBy());
            }
        }

        return getByUserId(userId);
    }

    /** Đổi role của 1 dòng phân quyền (không đổi user). */
    @Transactional
    public UserRoleResponse update(UUID id, UserRoleRequest request) {

        if (request == null) {
            throw badRequest("Dữ liệu cập nhật không được để trống");
        }

        UserRole userRole = requireUserRole(id);

        if (request.getRoleId() != null) {
            Role role = requireRole(request.getRoleId());

            if (!role.getId().equals(userRole.getRoleId())
                    && hasActiveAssignment(userRole.getUserId(), role.getId())) {
                throw conflict("User đã có role '" + role.getCode() + "'");
            }
            userRole.setRoleId(role.getId());
        }

        if (request.getUpdatedBy() != null) {
            userRole.setUpdatedBy(request.getUpdatedBy());
        }
        userRole.setUpdatedAt(LocalDateTime.now());

        return toResponses(List.of(userRoleRepository.save(userRole))).get(0);
    }

    /** Bật / tắt 1 dòng phân quyền. */
    @Transactional
    public UserRoleResponse updateActive(UUID id, Boolean active) {

        if (active == null) {
            throw badRequest("Trạng thái active là thông tin bắt buộc");
        }

        UserRole userRole = requireUserRole(id);

        userRole.setIsActive(active);
        userRole.setUpdatedAt(LocalDateTime.now());

        return toResponses(List.of(userRoleRepository.save(userRole))).get(0);
    }

    /** Gỡ role khỏi user (xoá mềm theo id dòng phân quyền). */
    @Transactional
    public void revoke(UUID id) {

        UserRole userRole = requireUserRole(id);
        softDelete(userRole, LocalDateTime.now(), null);
    }

    /** Gỡ 1 role cụ thể khỏi user. */
    @Transactional
    public void revokeRole(UUID userId, UUID roleId) {

        requireUser(userId);
        requireRole(roleId);

        UserRole userRole = userRoleRepository
                .findFirstByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)
                .orElseThrow(() -> notFound(
                        "User " + userId + " không có role " + roleId));

        softDelete(userRole, LocalDateTime.now(), null);
    }

    /** Khôi phục 1 dòng phân quyền đã xoá mềm. */
    @Transactional
    public UserRoleResponse restore(UUID id) {

        if (id == null) {
            throw badRequest("Id phân quyền không được để trống");
        }

        UserRole userRole = userRoleRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy phân quyền: " + id));

        if (userRole.getDeletedAt() == null) {
            throw conflict("Phân quyền chưa bị gỡ nên không cần khôi phục");
        }

        requireUser(userRole.getUserId());
        requireRole(userRole.getRoleId());

        if (hasActiveAssignment(userRole.getUserId(), userRole.getRoleId())) {
            throw conflict("User đã có role này ở một dòng phân quyền khác");
        }

        userRole.setDeletedAt(null);
        userRole.setDeletedBy(null);
        userRole.setIsActive(true);
        userRole.setUpdatedAt(LocalDateTime.now());

        return toResponses(List.of(userRoleRepository.save(userRole))).get(0);
    }

    // ==================== HELPERS ====================

    private boolean hasActiveAssignment(UUID userId, UUID roleId) {
        return userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId);
    }

    /**
     * Tạo dòng phân quyền mới, hoặc khôi phục dòng đã xoá mềm trước đó của cùng cặp (user, role)
     * để không sinh thêm bản ghi trùng.
     */
    private UserRole createOrRestoreAssignment(UUID userId, UUID roleId, UUID createdBy, UUID updatedBy) {

        LocalDateTime now = LocalDateTime.now();

        Optional<UserRole> softDeleted = userRoleRepository
                .findFirstByUserIdAndRoleIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId, roleId);

        UserRole userRole;

        if (softDeleted.isPresent()) {
            userRole = softDeleted.get();
            userRole.setDeletedAt(null);
            userRole.setDeletedBy(null);
            userRole.setIsActive(true);
            userRole.setUpdatedAt(now);
            if (updatedBy != null) {
                userRole.setUpdatedBy(updatedBy);
            }
        } else {
            if (userRoleRepository.existsByUserIdAndRoleIdAndDeletedAtIsNull(userId, roleId)) {
                throw conflict("User đã có role này");
            }
            userRole = new UserRole();
            userRole.setId(UUID.randomUUID());
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setIsActive(true);
            userRole.setCreatedAt(now);
            userRole.setUpdatedAt(now);
            userRole.setCreatedBy(createdBy);
        }

        return userRoleRepository.save(userRole);
    }

    private void softDelete(UserRole userRole, LocalDateTime deletedAt, UUID deletedBy) {
        userRole.setDeletedAt(deletedAt);
        userRole.setUpdatedAt(deletedAt);
        userRole.setIsActive(false);
        if (deletedBy != null) {
            userRole.setDeletedBy(deletedBy);
        }
        userRoleRepository.save(userRole);
    }

    private UserRole requireUserRole(UUID id) {
        if (id == null) {
            throw badRequest("Id phân quyền không được để trống");
        }
        return userRoleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> notFound("Không tìm thấy phân quyền: " + id));
    }

    private User requireUser(UUID userId) {
        if (userId == null) {
            throw badRequest("userId là thông tin bắt buộc");
        }
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> notFound("Không tìm thấy user: " + userId));
    }

    private Role requireRole(UUID roleId) {
        if (roleId == null) {
            throw badRequest("roleId là thông tin bắt buộc");
        }
        return roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> notFound("Không tìm thấy role: " + roleId));
    }

    /** Lấy danh sách roleId không trùng; allowEmpty = false thì danh sách rỗng bị coi là lỗi. */
    private Set<UUID> distinctRoleIds(List<UUID> roleIds, boolean allowEmpty) {

        Set<UUID> distinct = new LinkedHashSet<>();

        if (roleIds != null) {
            roleIds.stream()
                    .filter(Objects::nonNull)
                    .forEach(distinct::add);
        }

        if (distinct.isEmpty() && !allowEmpty) {
            throw badRequest("Danh sách roleIds không được để trống");
        }

        return distinct;
    }

    private List<UserRoleResponse> toResponses(List<UserRole> userRoles) {

        if (userRoles.isEmpty()) {
            return List.of();
        }

        Set<UUID> userIds = userRoles.stream()
                .map(UserRole::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, Role> roles = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        return userRoles.stream()
                .map(userRole -> toResponse(
                        userRole,
                        users.get(userRole.getUserId()),
                        roles.get(userRole.getRoleId())))
                .toList();
    }

    private UserRoleResponse toResponse(UserRole userRole, User user, Role role) {

        UserRoleResponse response = new UserRoleResponse();

        response.setId(userRole.getId());
        response.setUserId(userRole.getUserId());
        response.setRoleId(userRole.getRoleId());
        response.setIsActive(userRole.getIsActive());
        response.setCreatedAt(userRole.getCreatedAt());
        response.setUpdatedAt(userRole.getUpdatedAt());

        if (user != null) {
            response.setUsername(user.getUsername());
            response.setFullName(user.getFullName());
        }
        if (role != null) {
            response.setRoleCode(role.getCode());
            response.setRoleName(role.getName());
        }

        return response;
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
