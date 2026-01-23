package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.PropertyPoliciesResponseDTO;
import com.example.smart_booking_system.dto.request.PropertyPoliciesRequestDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.PropertyPolicies;
import com.example.smart_booking_system.repository.PropertyPoliciesRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyPoliciesService {

    private final PropertyPoliciesRepository policiesRepo;
    private final PropertyRepository propertyRepo;

    // ==========================
    // 1) ADD POLICY
    // ==========================
    public PropertyPoliciesResponseDTO addPolicies(int propertyId, PropertyPoliciesRequestDTO req) {

        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Nếu đã có policy thì không cho add nữa
        if (policiesRepo.findByPropertyId(propertyId) != null) {
            throw new RuntimeException("Policies already exist. Use update instead.");
        }

        PropertyPolicies policies = new PropertyPolicies();
        policies.setPropertyId(property);

        mapFields(req, policies);

        policiesRepo.save(policies);

        return convertToDTO(policies);
    }

    // ==========================
    // 2) UPDATE POLICY
    // ==========================
    public PropertyPoliciesResponseDTO updatePolicies(int propertyId, PropertyPoliciesRequestDTO req) {

        PropertyPolicies policies = policiesRepo.findByPropertyId(propertyId);

        if (policies == null) {
            throw new RuntimeException("Policies not found for this property");
        }

        // ==== UPDATE FIELD NÀO ĐƯỢC GỬI LÊN, GIỮ NGUYÊN FIELD KHÁC ====

        if (req.getPetPolicyDescription() != null) {
            policies.setPetPolicyDescription(req.getPetPolicyDescription());
        }
        policies.setPetsAllowed(req.isPetsAllowed());

        if (req.getSmokingPolicyDescription() != null) {
            policies.setSmokingPolicyDescription(req.getSmokingPolicyDescription());
        }
        policies.setSmokingAllowed(req.isSmokingAllowed());

        if (req.getChildrenPolicyDescription() != null) {
            policies.setChildrenPolicyDescription(req.getChildrenPolicyDescription());
        }
        policies.setChildrenAllowed(req.isChildrenAllowed());

        // ✅ SỬA: Update Check-in Time (Single)
        if (req.getCheckInTime() != null) {
            policies.setCheckInTime(req.getCheckInTime());
        }

        // ✅ SỬA: Update Check-out Time (Single)
        if (req.getCheckOutTime() != null) {
            policies.setCheckOutTime(req.getCheckOutTime());
        }

        if (req.getQuietHours() != null) {
            policies.setQuietHours(req.getQuietHours());
        }

        // Cancellation
        policies.setAllowFreeCancellation(req.isAllowFreeCancellation());

        if (req.getFreeCancellationDays() != null) {
            policies.setFreeCancellationDays(req.getFreeCancellationDays());
        }
        if (req.getCancellationPolicyDescription() != null) {
            policies.setCancellationPolicyDescription(req.getCancellationPolicyDescription());
        }

        // Prepayment
        policies.setRequiresPrepayment(req.isRequiresPrepayment());

        if (req.getPrepaymentPolicy() != null) {
            policies.setPrepaymentPolicy(req.getPrepaymentPolicy());
        }

        // Deposit
        policies.setSecurityDepositRequired(req.isSecurityDepositRequired());

        if (req.getSecurityDepositAmount() != null) {
            policies.setSecurityDepositAmount(req.getSecurityDepositAmount());
        }
        if (req.getSecurityDepositDescription() != null) {
            policies.setSecurityDepositDescription(req.getSecurityDepositDescription());
        }

        if (req.getMinimumAge() != null) {
            policies.setMinimumAge(req.getMinimumAge());
        }

        policiesRepo.save(policies);

        return convertToDTO(policies);
    }

    // ==========================
    // 3) GET POLICY
    // ==========================
    public PropertyPoliciesResponseDTO getPoliciesByPropertyId(int propertyId) {

        PropertyPolicies policies = policiesRepo.findByPropertyId(propertyId);

        if (policies == null) {
            throw new RuntimeException("Policies not found for propertyId " + propertyId);
        }

        return convertToDTO(policies);
    }

    // ===========================================
    // SUPPORT METHOD — MAP DTO → ENTITY
    // ===========================================
    private void mapFields(PropertyPoliciesRequestDTO req, PropertyPolicies policies) {

        policies.setPetsAllowed(req.isPetsAllowed());
        policies.setPetPolicyDescription(req.getPetPolicyDescription());

        policies.setSmokingAllowed(req.isSmokingAllowed());
        policies.setSmokingPolicyDescription(req.getSmokingPolicyDescription());

        policies.setChildrenAllowed(req.isChildrenAllowed());
        policies.setChildrenPolicyDescription(req.getChildrenPolicyDescription());

        // ✅ SỬA: Map Check-in/out Time
        policies.setCheckInTime(req.getCheckInTime());
        policies.setCheckOutTime(req.getCheckOutTime());

        policies.setQuietHours(req.getQuietHours());

        policies.setAllowFreeCancellation(req.isAllowFreeCancellation());
        policies.setFreeCancellationDays(req.getFreeCancellationDays());
        policies.setCancellationPolicyDescription(req.getCancellationPolicyDescription());

        policies.setRequiresPrepayment(req.isRequiresPrepayment());
        policies.setPrepaymentPolicy(req.getPrepaymentPolicy());

        policies.setSecurityDepositRequired(req.isSecurityDepositRequired());
        policies.setSecurityDepositAmount(req.getSecurityDepositAmount());
        policies.setSecurityDepositDescription(req.getSecurityDepositDescription());

        policies.setMinimumAge(req.getMinimumAge());
    }

    // ===========================================
    // SUPPORT METHOD — ENTITY → DTO
    // ===========================================
    private PropertyPoliciesResponseDTO convertToDTO(PropertyPolicies p) {

        PropertyPoliciesResponseDTO dto = new PropertyPoliciesResponseDTO();

        dto.setPolicyId(p.getPolicyId());
        dto.setPropertyId(p.getPropertyId().getPropertyId());

        dto.setPetsAllowed(p.isPetsAllowed());
        dto.setPetPolicyDescription(p.getPetPolicyDescription());

        dto.setSmokingAllowed(p.isSmokingAllowed());
        dto.setSmokingPolicyDescription(p.getSmokingPolicyDescription());

        dto.setChildrenAllowed(p.isChildrenAllowed());
        dto.setChildrenPolicyDescription(p.getChildrenPolicyDescription());

        // ✅ SỬA: Convert Check-in/out Time
        dto.setCheckInTime(p.getCheckInTime());
        dto.setCheckOutTime(p.getCheckOutTime());

        dto.setQuietHours(p.getQuietHours());

        dto.setAllowFreeCancellation(p.isAllowFreeCancellation());
        dto.setFreeCancellationDays(p.getFreeCancellationDays());
        dto.setCancellationPolicyDescription(p.getCancellationPolicyDescription());

        dto.setRequiresPrepayment(p.isRequiresPrepayment());
        dto.setPrepaymentPolicy(p.getPrepaymentPolicy());

        dto.setSecurityDepositRequired(p.isSecurityDepositRequired());
        dto.setSecurityDepositAmount(p.getSecurityDepositAmount());
        dto.setSecurityDepositDescription(p.getSecurityDepositDescription());

        dto.setMinimumAge(p.getMinimumAge());

        return dto;
    }
}