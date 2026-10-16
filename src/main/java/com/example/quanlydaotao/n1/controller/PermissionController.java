package com.example.quanlydaotao.n1.controller;

import com.example.quanlydaotao.n1.entity.Permission;
import com.example.quanlydaotao.n1.service.PermissionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API danh mục quyền chức năng chi tiết của hệ thống.
 *
 * GET    /api/permissions                  -> danh sách quyền (có thể kèm ?keyword=)
 * GET    /api/permissions/{id}             -> chi tiết theo id
 * GET    /api/permissions/code/{code}      -> chi tiết theo mã quyền
 * GET    /api/permissions/name/{name}      -> danh sách theo tên hiển thị
 * GET    /api/permissions/module/{module}  -> danh sách theo nhóm chức năng
 * GET    /api/permissions/active           -> danh sách quyền đang hoạt động
 * POST   /api/permissions                  -> thêm quyền (201)
 * PUT    /api/permissions/{id}             -> cập nhật quyền
 * PATCH  /api/permissions/{id}/active      -> bật/tắt quyền (?active=true|false)
 * PATCH  /api/permissions/{id}/restore     -> khôi phục quyền đã xoá mềm
 * DELETE /api/permissions/{id}             -> xoá mềm quyền
 */
@RestController
@RequestMapping("/api/permissions")
@CrossOrigin("*")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // ==================== GET ====================

    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions(
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(
                permissionService.searchPermissions(keyword)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<List<Permission>> getActivePermissions() {

        return ResponseEntity.ok(
                permissionService.getActivePermissions()
        );
    }

    @GetMapping("/module/{module}")
    public ResponseEntity<List<Permission>> getByModule(
            @PathVariable String module) {

        return ResponseEntity.ok(
                permissionService.getByModule(module)
        );
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Permission> getByCode(
            @PathVariable String code) {

        return ResponseEntity.ok(
                permissionService.getByCode(code)
        );
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<List<Permission>> getByName(
            @PathVariable String name) {

        return ResponseEntity.ok(
                permissionService.getByName(name)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                permissionService.getPermissionById(id)
        );
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<Permission> createPermission(
            @RequestBody Permission permission) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(permissionService.createPermission(permission));
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    public ResponseEntity<Permission> updatePermission(
            @PathVariable UUID id,
            @RequestBody Permission permission) {

        return ResponseEntity.ok(
                permissionService.updatePermission(id, permission)
        );
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Permission> updateActive(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                permissionService.updateActive(id, active)
        );
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Permission> restorePermission(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                permissionService.restorePermission(id)
        );
    }

    // ==================== DELETE (soft) ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePermission(
            @PathVariable UUID id) {

        permissionService.deletePermission(id);

        return ResponseEntity.ok("Xóa permission thành công");
    }
}
