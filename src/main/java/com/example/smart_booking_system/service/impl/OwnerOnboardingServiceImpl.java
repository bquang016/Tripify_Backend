package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.request.*;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import com.example.smart_booking_system.enums.RoomStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.OwnerOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerOnboardingServiceImpl implements OwnerOnboardingService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyPoliciesRepository propertyPoliciesRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final FileStorageService fileStorageService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional
    public void registerFull(
            String userId,
            FullOnboardingRequest request,
            MultipartFile avatar,
            MultipartFile cccdFront,
            MultipartFile cccdBack,
            List<MultipartFile> propertyImages,
            MultipartFile businessLicenseImage,
            List<MultipartFile> unitImages
    ) {
        // === PRE-VALIDATION ===
        OnboardingPropertyInfoRequest propertyInfo = request.getPropertyInfo();
        if (propertyInfo == null) throw new BadRequestException("Property information is required.");
        if (propertyImages == null || propertyImages.size() < 3) throw new BadRequestException("Property must have at least 3 images.");
        if (businessLicenseImage == null || businessLicenseImage.isEmpty()) throw new BadRequestException("Business license image is required.");
        PropertyType propertyType = propertyInfo.getPropertyType();
        if ((propertyType == PropertyType.VILLA || propertyType == PropertyType.HOMESTAY) && (unitImages == null || unitImages.isEmpty())) {
            throw new BadRequestException("Unit images are required for Villa or Homestay.");
        }

        // === STEP 1: UPDATE USER & USER DETAIL ===
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        owner.setFullName(request.getFullName());
        owner.setPhoneNumber(request.getPhoneNumber()); // Correct: Belongs to User
        userRepository.save(owner);

        UserDetail userDetail = userDetailRepository.findByUser(owner)
                .orElse(new UserDetail());
        userDetail.setUser(owner);
        // userDetail.setPhoneNumber(request.getPhoneNumber()); // Incorrect: Move to User
        userDetail.setIdentityCardNumber(request.getIdentityCardNumber());
        if (StringUtils.hasText(request.getDateOfBirth())) {
            userDetail.setDateOfBirth(LocalDate.parse(request.getDateOfBirth(), DATE_FORMATTER));
        }
        userDetail.setGender(request.getGender());
        userDetail.setAddress(request.getAddress());
        userDetail.setCity(request.getCity());

        // Upload user-related images
        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = fileStorageService.storeImageFile(avatar, "avatars/" + userId);
            userDetail.setProfilePhotoUrl(avatarUrl); // Correct setter
        }
        String cccdFrontUrl = fileStorageService.storeImageFile(cccdFront, "cccd/" + userId);
        String cccdBackUrl = fileStorageService.storeImageFile(cccdBack, "cccd/" + userId);
        userDetail.setCccdFrontUrl(cccdFrontUrl); // Correct setter
        userDetail.setCccdBackUrl(cccdBackUrl);   // Correct setter
        userDetailRepository.save(userDetail);


        // === STEP 2: CREATE PROPERTY AND RELATED ENTITIES ===
        Property property = new Property();
        property.setOwner(owner);
        property.setPropertyType(propertyInfo.getPropertyType());
        property.setPropertyName(propertyInfo.getPropertyName());
        property.setDescription(propertyInfo.getDescription());
        property.setAddress(propertyInfo.getPropertyAddress());
        property.setCity(propertyInfo.getPropertyCity());
        property.setDistrict(propertyInfo.getPropertyDistrict());
        property.setWard(propertyInfo.getPropertyWard());
        property.setLatitude(BigDecimal.valueOf(propertyInfo.getLatitude()));
        property.setLongitude(BigDecimal.valueOf(propertyInfo.getLongitude()));
        property.setPropertyStatus(PropertyStatus.PENDING);
        property.setBusinessLicenseNumber(propertyInfo.getBusinessLicenseNumber());
        String licenseImageUrl = fileStorageService.storeImageFile(businessLicenseImage, "licenses/" + owner.getUserId());
        property.setBusinessLicenseImageUrl(licenseImageUrl);

        Property savedProperty = propertyRepository.save(property);

        // Property Images
        List<PropertyImage> propertyImageList = new ArrayList<>();
        for (MultipartFile file : propertyImages) {
            String imageUrl = fileStorageService.storeImageFile(file, "properties/" + savedProperty.getPropertyId());
            PropertyImage propertyImage = new PropertyImage();
            propertyImage.setProperty(savedProperty);
            propertyImage.setImageUrl(imageUrl);
            propertyImageList.add(propertyImage);
        }
        propertyImageRepository.saveAll(propertyImageList);

        // Property Policies
        PropertyPolicyRequest policyRequest = propertyInfo.getPolicies();
        if (policyRequest != null) {
            PropertyPolicies policies = new PropertyPolicies();
            policies.setPropertyId(savedProperty);
            policies.setCheckInTime(LocalTime.parse(policyRequest.getCheckInTime(), TIME_FORMATTER));
            policies.setCheckOutTime(LocalTime.parse(policyRequest.getCheckOutTime(), TIME_FORMATTER));
            policies.setMinimumAge(policyRequest.getMinimumAge());
            policies.setAllowFreeCancellation(policyRequest.isAllowFreeCancellation());
            policies.setFreeCancellationDays(policyRequest.getFreeCancellationDays());
            propertyPoliciesRepository.save(policies);
        }

        // Property Amenities
        if (propertyInfo.getAmenityIds() != null && !propertyInfo.getAmenityIds().isEmpty()) {
            List<Integer> amenityIds = propertyInfo.getAmenityIds().stream().map(Integer::parseInt).collect(Collectors.toList());
            List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
            List<PropertyAmenity> propertyAmenities = new ArrayList<>();
            for (Amenity amenity : amenities) {
                PropertyAmenity propertyAmenity = new PropertyAmenity();
                propertyAmenity.setProperty(savedProperty);
                propertyAmenity.setAmenity(amenity);
                propertyAmenities.add(propertyAmenity);
            }
            propertyAmenityRepository.saveAll(propertyAmenities);
        }

        // Conditional Room for VILLA/HOMESTAY
        if (propertyType == PropertyType.VILLA || propertyType == PropertyType.HOMESTAY) {
            Room unit = new Room();
            unit.setProperty(savedProperty);
            unit.setRoomName(propertyInfo.getUnitData() != null ? propertyInfo.getUnitData().getName() : "Căn " + savedProperty.getPropertyName());
            unit.setPricePerNight(propertyInfo.getPrice());
            unit.setWeekendPrice(propertyInfo.getWeekendPrice());
            unit.setCapacity(propertyInfo.getCapacity());
            unit.setArea(BigDecimal.valueOf(propertyInfo.getArea()));
            unit.setRoomStatus(RoomStatus.AVAILABLE);
            unit.setRoomAmount(1);
            Room savedUnit = roomRepository.save(unit);

            if (unitImages != null && !unitImages.isEmpty()) {
                List<RoomImage> roomImageList = new ArrayList<>();
                for (MultipartFile file : unitImages) {
                    String imageUrl = fileStorageService.storeImageFile(file, "rooms/" + savedUnit.getRoomId());
                    RoomImage roomImage = new RoomImage();
                    roomImage.setRoom(savedUnit);
                    roomImage.setImageUrl(imageUrl);
                    roomImageList.add(roomImage);
                }
                roomImageRepository.saveAll(roomImageList);
            }
        }

        // === STEP 3: CREATE PAYMENT DETAILS ===
        OnboardingPaymentInfoRequest paymentInfoRequest = request.getPaymentInfo();
        if (paymentInfoRequest != null) {
            PaymentDetail paymentDetail = paymentDetailRepository.findByUserUserId(userId).orElse(new PaymentDetail());
            paymentDetail.setUser(owner);
            paymentDetail.setPaymentMethod(paymentInfoRequest.getPaymentMethod());
            paymentDetail.setBankName(paymentInfoRequest.getBankName());
            paymentDetail.setAccountHolderName(paymentInfoRequest.getAccountHolderName());
            paymentDetail.setAccountNumber(paymentInfoRequest.getAccountNumber());
            paymentDetailRepository.save(paymentDetail);
        }
    }
}
