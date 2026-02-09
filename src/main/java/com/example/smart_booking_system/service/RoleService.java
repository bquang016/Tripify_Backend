package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.PermissionResponseDTO;
import com.example.smart_booking_system.dto.response.RoleResponseDTO;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RoleService {
    List<RoleResponseDTO> getAllRoles();
    Map<String, List<PermissionResponseDTO>> getAllPermissionsGrouped();
    RoleResponseDTO getRolePermissions(Long roleId);
    RoleResponseDTO updateRolePermissions(Long roleId, Set<String> permissionCodes);
}
