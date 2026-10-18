package com.example.quanlydaotao.n1.controller;

import com.example.quanlydaotao.n1.entity.User;
import com.example.quanlydaotao.n1.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users?active=true|false
     * Lấy user chưa xóa mềm; active không truyền thì lấy cả tài khoản bị khóa.
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(required = false) Boolean active) {

        return ResponseEntity.ok(
                userService.getUsers(active)
        );
    }

    /** GET /api/users/deleted - liệt kê các user đã xóa mềm. */
    @GetMapping("/deleted")
    public ResponseEntity<List<User>> getDeletedUsers() {
        return ResponseEntity.ok(userService.getDeletedUsers());
    }

    /** GET /api/users/page?page=0&size=20&active=true|false - danh sách có phân trang. */
    @GetMapping("/page")
    public ResponseEntity<Page<User>> getUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(userService.getUsersPage(page, size, active));
    }

    /** GET /api/users/{id} - lấy chi tiết một user chưa xóa mềm. */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    /** GET /api/users/username/{username} - tìm user theo tên đăng nhập. */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getByUsername(
            @PathVariable String username) {

        return ResponseEntity.ok(
                userService.getByUsername(username)
        );
    }

    /** GET /api/users/search?keyword=... - tìm theo username, họ tên, email hoặc số điện thoại. */
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(
                userService.searchUsers(keyword)
        );
    }

    /**
     * POST /api/users - tạo tài khoản.
     * JSON nhận thông tin User; passwordHash là giá trị lưu vào cột password.
     */
    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.createUser(user)
        );
    }

    /** PUT /api/users/{id} - cập nhật thông tin hồ sơ được gửi lên. */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.updateUser(id, user)
        );
    }

    /**
     * DELETE /api/users/{id}?deletedBy=<UUID> - xóa mềm và tùy chọn ghi người xóa.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID deletedBy) {

        userService.deleteUser(id, deletedBy);

        return ResponseEntity.ok(
                "Xóa user thành công"
        );
    }

    /** PATCH /api/users/{id}/restore - bỏ dấu xóa mềm và kích hoạt lại tài khoản. */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<User> restoreUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.restoreUser(id));
    }

    /**
     * PATCH /api/users/{id}/password - cập nhật passwordHash đã được mã hóa.
     * Body: {"passwordHash":"..."}.
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<User> updatePassword(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String passwordHash = request == null ? null : request.get("passwordHash");
        return ResponseEntity.ok(userService.updatePasswordHash(id, passwordHash));
    }

    /** PATCH /api/users/{id}/active?active=true|false - đổi trạng thái hiệu lực. */
    @PatchMapping("/{id}/active")
    public ResponseEntity<User> updateActive(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                userService.updateActive(id, active)
        );
    }

    /** Alias tương thích; thực hiện cùng chức năng với endpoint /active. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<User> updateStatus(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                userService.updateActive(id, active)
        );
    }
}