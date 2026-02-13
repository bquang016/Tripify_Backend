package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.response.PermissionResponseDTO;
import com.example.smart_booking_system.dto.response.RoleResponseDTO;
import com.example.smart_booking_system.entity.Permission;
import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.PermissionRepository;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PermissionResponseDTO>> getAllPermissionsGrouped() {
        return permissionRepository.findAll().stream()
                .map(this::convertToPermissionDTO)
                .collect(Collectors.groupingBy(PermissionResponseDTO::getGroupName));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponseDTO getRolePermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        return convertToRoleDTO(role);
    }

    @Override
    @Transactional
    public RoleResponseDTO updateRolePermissions(Long roleId, Set<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSuper())) {
            throw new BadRequestException("Cannot update permissions for Super Admin role.");
        }

        Set<Permission> permissions = permissionCodes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found with code: " + code)))
                .collect(Collectors.toSet());

        role.setPermissions(permissions);
        Role savedRole = roleRepository.save(role);
        return convertToRoleDTO(savedRole);
    }

    private RoleResponseDTO convertToRoleDTO(Role role) {
        return RoleResponseDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSuper(role.getIsSuper())
                .permissions(role.getPermissions().stream()
                        .map(this::convertToPermissionDTO)
                        .collect(Collectors.toSet()))
                .build();
    }

    private PermissionResponseDTO convertToPermissionDTO(Permission permission) {
        return PermissionResponseDTO.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .groupName(permission.getGroupName())
                .build();
    }
}
