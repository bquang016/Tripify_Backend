package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.PropertyPoliciesRequestDTO;
import com.example.smart_booking_system.service.PropertyPoliciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/properties")  // Base URL theo chuẩn RESTful
@RequiredArgsConstructor
public class PropertyPoliciesController {

    private final PropertyPoliciesService policyService;

    // ==========================
    // 1) ADD POLICY
    // ==========================
    @PostMapping("/{propertyId}/policies")
    public ResponseEntity<?> addPolicies(
            @PathVariable int propertyId,
            @RequestBody PropertyPoliciesRequestDTO req) {

        try {
            return ResponseEntity.ok(policyService.addPolicies(propertyId, req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================
    // 2) UPDATE POLICY
    // ==========================
    @PutMapping("/{propertyId}/policies")
    public ResponseEntity<?> updatePolicies(
            @PathVariable int propertyId,
            @RequestBody PropertyPoliciesRequestDTO req) {

        try {
            return ResponseEntity.ok(policyService.updatePolicies(propertyId, req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================
    // 3) READ POLICY BY PROPERTY
    // ==========================
    @GetMapping("/{propertyId}/policies")
    public ResponseEntity<?> getPolicies(@PathVariable int propertyId) {

        try {
            return ResponseEntity.ok(policyService.getPoliciesByPropertyId(propertyId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
