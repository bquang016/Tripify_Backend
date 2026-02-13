package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.internal.OwnerApplicationData;
import com.example.smart_booking_system.dto.request.OwnerApplicationRequest;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.RoomStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.InternalServerException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.OwnerApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDetailRepository userDetailRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyPoliciesRepository propertyPoliciesRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional
    public void approveApplication(Long applicationId, String adminUsername) {
        OwnerApplication application = ownerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("This application has already been reviewed.");
        }

        try {
            OwnerApplicationData data = objectMapper.readValue(application.getApplicationData(), OwnerApplicationData.class);

            // 1. Generate random password
            String randomPassword = generateRandomPassword();

            // 2. Create and save User
            Role ownerRole = roleRepository.findByRoleName("OWNER")
                    .orElseThrow(() -> new InternalServerException("Role 'OWNER' not found."));
            User newUser = new User();
            newUser.setEmail(application.getEmail());
            newUser.setFullName(data.getFullName());
            newUser.setPhoneNumber(data.getPhoneNumber());
            newUser.setPasswordHash(passwordEncoder.encode(randomPassword));
            newUser.addRole(ownerRole);
            newUser.setIsEmailVerified(true);
            newUser.setStatus("ACTIVE");
            User savedUser = userRepository.save(newUser);

            // 3. Create and save UserDetail
            UserDetail userDetail = new UserDetail();
            userDetail.setUser(savedUser);
            userDetail.setIdentityCardNumber(data.getIdentityCardNumber());
            userDetail.setDateOfBirth(LocalDate.parse(data.getDateOfBirth(), DATE_FORMATTER));
            userDetail.setGender(data.getGender());
            userDetail.setAddress(data.getAddress());
            userDetail.setCity(data.getCity());
            userDetail.setProfilePhotoUrl(data.getAvatarUrl());
            userDetail.setCccdFrontUrl(data.getCccdFrontUrl());
            userDetail.setCccdBackUrl(data.getCccdBackUrl());
            userDetailRepository.save(userDetail);
            
            // 4. Create and save Property and related entities
            OwnerApplicationRequest.PropertyInfo propertyInfo = data.getPropertyInfo();
            Property property = new Property();
            property.setOwner(savedUser);
            property.setPropertyType(propertyInfo.getPropertyType());
            property.setPropertyName(propertyInfo.getPropertyName());
            property.setDescription(propertyInfo.getDescription());
            property.setAddress(propertyInfo.getPropertyAddress());
            property.setCity(propertyInfo.getPropertyCity());
            property.setDistrict(propertyInfo.getPropertyDistrict());
            property.setWard(propertyInfo.getPropertyWard());
            property.setLatitude(java.math.BigDecimal.valueOf(propertyInfo.getLatitude()));
            property.setLongitude(java.math.BigDecimal.valueOf(propertyInfo.getLongitude()));
            property.setBusinessLicenseNumber(propertyInfo.getBusinessLicenseNumber());
            property.setBusinessLicenseImage(data.getBusinessLicenseImage());
            property.setPropertyStatus(PropertyStatus.APPROVE);
            property.setActive(true); // Activate the property immediately
            Property savedProperty = propertyRepository.save(property);

            // Property Images
            if (data.getPropertyImageUrls() != null) {
                List<PropertyImage> propertyImageList = data.getPropertyImageUrls().stream().map(url -> {
                    PropertyImage img = new PropertyImage();
                    img.setProperty(savedProperty);
                    img.setImageUrl(url);
                    return img;
                }).collect(Collectors.toList());
                propertyImageRepository.saveAll(propertyImageList);
            }

            // Property Policies
            OwnerApplicationRequest.PolicyData policyData = propertyInfo.getPolicies();
            if (policyData != null) {
                PropertyPolicies policies = new PropertyPolicies();
                policies.setPropertyId(savedProperty);
                policies.setCheckInTime(LocalTime.parse(policyData.getCheckInTime(), TIME_FORMATTER));
                policies.setCheckOutTime(LocalTime.parse(policyData.getCheckOutTime(), TIME_FORMATTER));
                policies.setMinimumAge(policyData.getMinimumAge());
                policies.setAllowFreeCancellation(policyData.isAllowFreeCancellation());
                policies.setFreeCancellationDays(policyData.getFreeCancellationDays());
                propertyPoliciesRepository.save(policies);
            }
            
            // Property Amenities
            if (propertyInfo.getAmenityIds() != null && !propertyInfo.getAmenityIds().isEmpty()) {
                List<Amenity> amenities = amenityRepository.findAllById(propertyInfo.getAmenityIds().stream().map(Integer::parseInt).collect(Collectors.toList()));
                List<PropertyAmenity> propertyAmenities = amenities.stream().map(amenity -> {
                    PropertyAmenity pa = new PropertyAmenity();
                    pa.setProperty(savedProperty);
                    pa.setAmenity(amenity);
                    return pa;
                }).collect(Collectors.toList());
                propertyAmenityRepository.saveAll(propertyAmenities);
            }

            // Rooms/Units for VILLA/HOMESTAY
            if (propertyInfo.getPropertyType() == com.example.smart_booking_system.enums.PropertyType.VILLA ||
                propertyInfo.getPropertyType() == com.example.smart_booking_system.enums.PropertyType.HOMESTAY) {
                Room unit = new Room();
                unit.setProperty(savedProperty);
                unit.setRoomName(propertyInfo.getUnitData() != null ? propertyInfo.getUnitData().getName() : "Căn " + savedProperty.getPropertyName());
                unit.setPricePerNight(propertyInfo.getPrice());
                unit.setWeekendPrice(propertyInfo.getWeekendPrice());
                unit.setCapacity(propertyInfo.getCapacity());
                unit.setArea(java.math.BigDecimal.valueOf(propertyInfo.getArea()));
                unit.setRoomStatus(RoomStatus.AVAILABLE);
                unit.setRoomAmount(1);
                Room savedUnit = roomRepository.save(unit);

                if (data.getUnitImageUrls() != null) {
                    List<RoomImage> roomImageList = data.getUnitImageUrls().stream().map(url -> {
                        RoomImage img = new RoomImage();
                        img.setRoom(savedUnit);
                        img.setImageUrl(url);
                        return img;
                    }).collect(Collectors.toList());
                    roomImageRepository.saveAll(roomImageList);
                }
            }

            // 5. Create PaymentDetail
            OwnerApplicationRequest.PaymentInfo paymentInfo = data.getPaymentInfo();
            if (paymentInfo != null) {
                PaymentDetail paymentDetail = new PaymentDetail();
                paymentDetail.setUser(savedUser);
                paymentDetail.setPaymentMethod(paymentInfo.getPaymentMethod());
                paymentDetail.setBankName(paymentInfo.getBankName());
                paymentDetail.setAccountHolderName(paymentInfo.getAccountHolderName());
                paymentDetail.setAccountNumber(paymentInfo.getAccountNumber());
                paymentDetailRepository.save(paymentDetail);
            }
            
            // 6. Update Application Status
            application.setStatus(ApplicationStatus.APPROVED);
            application.setReviewedBy(adminUsername);
            application.setReviewedAt(LocalDateTime.now());
            ownerApplicationRepository.save(application);

            // 7. Send Approval Email
            Context context = new Context();
            context.setVariable("applicantName", data.getFullName());
            context.setVariable("email", application.getEmail());
            context.setVariable("password", randomPassword);
            emailService.sendHtmlEmail(application.getEmail(), "Chúc mừng! Đơn đăng ký đối tác của bạn đã được phê duyệt", "email/application-approved", context);

        } catch (Exception e) {
            throw new InternalServerException("Failed to approve application: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void rejectApplication(Long applicationId, String reason, String adminUsername) {
        OwnerApplication application = ownerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("This application has already been reviewed.");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        application.setReviewedBy(adminUsername);
        application.setReviewedAt(LocalDateTime.now());
        ownerApplicationRepository.save(application);

        String applicantName = "Applicant";
        try {
            OwnerApplicationData data = objectMapper.readValue(application.getApplicationData(), OwnerApplicationData.class);
            applicantName = data.getFullName();
        } catch (Exception e) {
            // Ignore error, use default name
        }

        Context context = new Context();
        context.setVariable("applicantName", applicantName);
        context.setVariable("reason", reason);
        emailService.sendHtmlEmail(application.getEmail(), "Thông báo: Đơn đăng ký đối tác của bạn đã bị từ chối", "email/application-rejected", context);

        // Delete the application to free up the email for future registrations
        ownerApplicationRepository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status) {
        return ownerApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == status)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerApplicationDTO getApplicationDetail(Long id) {
        OwnerApplication application = ownerApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
        return convertToDTO(application);
    }

    private OwnerApplicationDTO convertToDTO(OwnerApplication app) {
        // --- FIX START: Kiểm tra null trước khi parse ---
        if (app.getApplicationData() == null) {
            OwnerApplicationDTO dto = new OwnerApplicationDTO();
            dto.setId(app.getId());
            dto.setStatus(app.getStatus());
            dto.setApplicantEmail(app.getEmail());
            dto.setAdminReason("Dữ liệu đơn đăng ký bị lỗi (NULL)"); // Đánh dấu để Admin biết
            return dto;
        }
        // --- FIX END ---

        ApplicationStatus newStatus = reviewDTO.getStatus();
        if (newStatus == null)
            throw new IllegalArgumentException("Trạng thái mới không được để trống.");

        if (newStatus != ApplicationStatus.APPROVED && newStatus != ApplicationStatus.REJECTED)
            throw new IllegalArgumentException("Chỉ được chuyển sang APPROVED hoặc REJECTED.");

        application.setAdminReason(reviewDTO.getReason());
        application.setStatus(newStatus);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);

        User applicant = application.getUserId();

        if (newStatus == ApplicationStatus.APPROVED) {
            Role ownerRole = roleRepository.findByName("OWNER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_OWNER"));

            applicant.addRole(ownerRole);
            userRepository.save(applicant);
        }
        try {
            OwnerApplicationData data = objectMapper.readValue(app.getApplicationData(), OwnerApplicationData.class);
            OwnerApplicationDTO dto = new OwnerApplicationDTO();

            // --- CÁC TRƯỜNG CƠ BẢN ---
            dto.setId(app.getId());
            dto.setStatus(app.getStatus());
            dto.setApplicantEmail(app.getEmail());
            dto.setCreatedAt(app.getCreatedAt());
            dto.setReviewedAt(app.getReviewedAt());
            dto.setAdminReason(app.getRejectionReason());
            if (app.getReviewedBy() != null) {
                dto.setReviewedByAdminName(app.getReviewedBy());
            }

            // --- MAPPING THÔNG TIN CÁ NHÂN (QUAN TRỌNG) ---
            if (data != null) { // Kiểm tra data null thêm 1 lần nữa cho chắc chắn
                dto.setApplicantFullName(data.getFullName());
                dto.setApplicantPhoneNumber(data.getPhoneNumber());
                dto.setApplicantAvatar(data.getAvatarUrl());
                dto.setPersonalIdCard(data.getIdentityCardNumber());
                dto.setGender(data.getGender());
                dto.setPermanentAddress(data.getAddress());
                dto.setHometownAddress(data.getCity());

                dto.setCardFrontImage(data.getCccdFrontUrl());
                dto.setCardBackImage(data.getCccdBackUrl());
                dto.setBusinessLicenseImage(data.getBusinessLicenseImage());

                if (data.getPropertyInfo() != null) {
                    dto.setBusinessLicenseNumber(data.getPropertyInfo().getBusinessLicenseNumber());
                }

                if (data.getDateOfBirth() != null) {
                    try {
                        dto.setApplicantDob(LocalDate.parse(data.getDateOfBirth(), DATE_FORMATTER));
                    } catch (Exception e) {
                        // Ignore date parse error
                    }
                }

                // --- MAPPING PROPERTY INFO ---
                OwnerApplicationRequest.PropertyInfo prop = data.getPropertyInfo();
                if (prop != null) {
                    OwnerApplicationDTO.PropertyInfoDTO propDTO = new OwnerApplicationDTO.PropertyInfoDTO();
                    propDTO.setPropertyName(prop.getPropertyName());
                    propDTO.setPropertyType(prop.getPropertyType() != null ? prop.getPropertyType().toString() : "");
                    propDTO.setDescription(prop.getDescription());
                    propDTO.setPropertyAddress(prop.getPropertyAddress());
                    propDTO.setPropertyCity(prop.getPropertyCity());
                    propDTO.setPropertyDistrict(prop.getPropertyDistrict());
                    propDTO.setPropertyWard(prop.getPropertyWard());
                    propDTO.setLatitude(prop.getLatitude());
                    propDTO.setLongitude(prop.getLongitude());
                    propDTO.setBusinessLicenseNumber(prop.getBusinessLicenseNumber());

                    propDTO.setPrice(prop.getPrice() != null ? prop.getPrice().doubleValue() : 0.0);
                    propDTO.setWeekendPrice(prop.getWeekendPrice() != null ? prop.getWeekendPrice().doubleValue() : 0.0);
                    propDTO.setCapacity(prop.getCapacity());
                    propDTO.setArea(prop.getArea() != null ? prop.getArea() : 0.0);

                    propDTO.setPropertyImageUrls(data.getPropertyImageUrls());

                    // Amenities
                    if (prop.getAmenityIds() != null && !prop.getAmenityIds().isEmpty()) {
                        List<Integer> amenityIds = prop.getAmenityIds().stream()
                                .map(id -> {
                                    try { return Integer.parseInt(id.toString()); } catch (NumberFormatException e) { return null; }
                                })
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!amenityIds.isEmpty()) {
                            List<String> amenityNames = amenityRepository.findAllById(amenityIds).stream()
                                    .map(Amenity::getAmenityName)
                                    .collect(Collectors.toList());
                            propDTO.setAmenityNames(amenityNames);
                        }
                    }

                    // Policies
                    if (prop.getPolicies() != null) {
                        OwnerApplicationDTO.PoliciesDTO polDTO = new OwnerApplicationDTO.PoliciesDTO();
                        polDTO.setCheckInTime(prop.getPolicies().getCheckInTime());
                        polDTO.setCheckOutTime(prop.getPolicies().getCheckOutTime());
                        polDTO.setMinimumAge(prop.getPolicies().getMinimumAge());
                        polDTO.setAllowFreeCancellation(prop.getPolicies().isAllowFreeCancellation());
                        polDTO.setFreeCancellationDays(prop.getPolicies().getFreeCancellationDays());
                        propDTO.setPolicies(polDTO);
                    }
                    dto.setPropertyInfo(propDTO);
                }

                // --- MAPPING PAYMENT INFO ---
                OwnerApplicationRequest.PaymentInfo pay = data.getPaymentInfo();
                if (pay != null) {
                    OwnerApplicationDTO.PaymentInfoDTO payDTO = new OwnerApplicationDTO.PaymentInfoDTO();
                    payDTO.setPaymentMethod(pay.getPaymentMethod());
                    payDTO.setBankName(pay.getBankName());
                    payDTO.setAccountHolderName(pay.getAccountHolderName());
                    payDTO.setAccountNumber(pay.getAccountNumber());
                    dto.setPaymentInfo(payDTO);
                }
            }

            return dto;
        } catch(Exception e) {
            e.printStackTrace(); // Log lỗi để debug
            OwnerApplicationDTO dto = new OwnerApplicationDTO();
            dto.setId(app.getId());
            dto.setStatus(app.getStatus());
            dto.setApplicantEmail(app.getEmail());
            dto.setAdminReason("Error parsing data: " + e.getMessage());
            return dto;
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        SecureRandom random = new SecureRandom();
        return random.ints(12, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());
    }
}