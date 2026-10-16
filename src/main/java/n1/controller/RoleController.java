package n1.controller;

import n1.entity.Role;
import n1.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin("*")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // =========================
    // GET ALL
    // =========================

    @GetMapping
    public ResponseEntity<List<Role>> getAll() {

        return ResponseEntity.ok(
                roleService.getAll()
        );
    }

    // =========================
    // GET BY ID
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<Role> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                roleService.getById(id)
        );
    }

    // =========================
    // CREATE
    // =========================

    @PostMapping
    public ResponseEntity<Role> create(
            @RequestBody Role role
    ) {

        Role newRole = roleService.create(role);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(newRole);
    }

    // =========================
    // UPDATE
    // =========================

    @PutMapping("/{id}")
    public ResponseEntity<Role> update(
            @PathVariable UUID id,
            @RequestBody Role role
    ) {

        return ResponseEntity.ok(
                roleService.update(id, role)
        );
    }

    // =========================
    // DELETE
    // =========================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable UUID id
    ) {

        roleService.delete(id);

        return ResponseEntity.ok(
                "Xóa role thành công"
        );
    }

    // =========================
    // RESTORE
    // =========================

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Role> restore(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                roleService.restore(id)
        );
    }
}