package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.PermissionResponseDTO;
import com.example.smart_booking_system.dto.response.RoleResponseDTO;
import com.example.smart_booking_system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<PermissionResponseDTO>>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissionsGrouped());
    }

    @GetMapping("/roles/{id}/permissions")
    public ResponseEntity<RoleResponseDTO> getRolePermissions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRolePermissions(id));
    }

    @PutMapping("/roles/{id}/permissions")
    public ResponseEntity<RoleResponseDTO> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionCodes) {
        return ResponseEntity.ok(roleService.updateRolePermissions(id, permissionCodes));
    }
}
