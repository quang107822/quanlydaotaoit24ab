package com.example.quanlydaotao.n1.controller;

import com.example.quanlydaotao.n1.dto.AssignRolesRequest;
import com.example.quanlydaotao.n1.dto.UserRoleRequest;
import com.example.quanlydaotao.n1.dto.UserRoleResponse;
import com.example.quanlydaotao.n1.entity.Role;
import com.example.quanlydaotao.n1.service.UserRoleService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API bảng trung gian user_roles: gán 1 hoặc nhiều vai trò (role) cho mỗi người dùng.
 *
 * GET    /api/user-roles                                  -> tất cả phân quyền
 * GET    /api/user-roles/{id}                              -> chi tiết 1 dòng phân quyền
 * GET    /api/user-roles/user/{userId}                     -> các role của user (kèm dòng đang tắt)
 * GET    /api/user-roles/user/{userId}/roles               -> danh sách role đang có hiệu lực của user
 * GET    /api/user-roles/role/{roleId}                     -> các user đang giữ role
 * POST   /api/user-roles                                   -> gán 1 role cho user (201)
 * POST   /api/user-roles/user/{userId}                     -> gán thêm nhiều role cho user (201)
 * PUT    /api/user-roles/user/{userId}                     -> đồng bộ toàn bộ role của user (201)
 * PUT    /api/user-roles/{id}                              -> đổi role của 1 dòng phân quyền
 * PATCH  /api/user-roles/{id}/active                       -> bật/tắt (?active=true|false)
 * PATCH  /api/user-roles/{id}/restore                      -> khôi phục dòng đã gỡ
 * DELETE /api/user-roles/{id}                              -> gỡ 1 dòng phân quyền (xoá mềm)
 * DELETE /api/user-roles/user/{userId}/role/{roleId}        -> gỡ 1 role cụ thể khỏi user (xoá mềm)
 */
@RestController
@RequestMapping("/api/user-roles")
@CrossOrigin("*")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    // ==================== GET ====================

    @GetMapping
    public ResponseEntity<List<UserRoleResponse>> getAll() {

        return ResponseEntity.ok(
                userRoleService.getAll()
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserRoleResponse>> getByUserId(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                userRoleService.getByUserId(userId)
        );
    }

    @GetMapping("/user/{userId}/roles")
    public ResponseEntity<List<Role>> getRolesByUserId(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                userRoleService.getRolesByUserId(userId)
        );
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<UserRoleResponse>> getByRoleId(
            @PathVariable UUID roleId) {

        return ResponseEntity.ok(
                userRoleService.getByRoleId(roleId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRoleResponse> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                userRoleService.getById(id)
        );
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<UserRoleResponse> assignRole(
            @RequestBody UserRoleRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userRoleService.assignRole(request));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<List<UserRoleResponse>> assignRoles(
            @PathVariable UUID userId,
            @RequestBody AssignRolesRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userRoleService.assignRoles(userId, request));
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<List<UserRoleResponse>> syncRoles(
            @PathVariable UUID userId,
            @RequestBody AssignRolesRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userRoleService.syncRoles(userId, request));
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    public ResponseEntity<UserRoleResponse> update(
            @PathVariable UUID id,
            @RequestBody UserRoleRequest request) {

        return ResponseEntity.ok(
                userRoleService.update(id, request)
        );
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<UserRoleResponse> updateActive(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                userRoleService.updateActive(id, active)
        );
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<UserRoleResponse> restore(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                userRoleService.restore(id)
        );
    }

    // ==================== DELETE (soft) ====================

    @DeleteMapping("/user/{userId}/role/{roleId}")
    public ResponseEntity<String> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {

        userRoleService.revokeRole(userId, roleId);

        return ResponseEntity.ok("Gỡ role khỏi user thành công");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> revoke(
            @PathVariable UUID id) {

        userRoleService.revoke(id);

        return ResponseEntity.ok("Gỡ phân quyền thành công");
    }
}
