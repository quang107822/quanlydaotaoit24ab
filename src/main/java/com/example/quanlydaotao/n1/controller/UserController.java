package com.example.quanlydaotao.n1.controller;

import com.example.quanlydaotao.n1.entity.User;
import com.example.quanlydaotao.n1.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                userService.getUserById(id)
        );
    }

    // GET BY USERNAME
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getByUsername(
            @PathVariable String username) {

        return ResponseEntity.ok(
                userService.getByUsername(username)
        );
    }

    // SEARCH
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(
                userService.searchUsers(keyword)
        );
    }

    // CREATE
    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.createUser(user)
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.updateUser(id, user)
        );
    }

    // DELETE SOFT
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable UUID id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(
                "Xóa user thành công"
        );
    }

    // ACTIVE / INACTIVE
    @PatchMapping("/{id}/active")
    public ResponseEntity<User> updateActive(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                userService.updateActive(id, active)
        );
    }

    // STATUS endpoint retained alongside the existing /active endpoint
    @PatchMapping("/{id}/status")
    public ResponseEntity<User> updateStatus(
            @PathVariable UUID id,
            @RequestParam Boolean active) {

        return ResponseEntity.ok(
                userService.updateActive(id, active)
        );
    }
}