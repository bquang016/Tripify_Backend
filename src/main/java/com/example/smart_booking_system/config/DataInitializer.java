package com.example.smart_booking_system.config;

import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AmenityRepository amenityRepository; // THÊM MỚI: Inject Repository

    @PostConstruct
    public void init() {
        // 1. Khởi tạo Roles
        initRoles();

        // 2. Khởi tạo Users
        initDefaultUsers();

        // 3. Khởi tạo Amenities (THÊM MỚI)
        initAmenities();
    }

    private void initRoles() {
        // Dùng tên không có tiền tố
        List<String> defaultRoles = List.of("CUSTOMER", "ADMIN", "OWNER");

        for (String roleName : defaultRoles) {
            roleRepository.findByRoleName(roleName)
                    .or(() -> {
                        Role role = new Role();
                        role.setRoleName(roleName);
                        roleRepository.save(role);
                        System.out.println("✅ Created default role: " + roleName);
                        return java.util.Optional.of(role);
                    });
        }
    }

    private void initDefaultUsers() {
        // Dùng tên không có tiền tố
        createAccountIfNotExists(
                "admin@travelmate.vn",
                    "Admin@123",
                "Admin FullName",
                Set.of("ADMIN", "CUSTOMER")
        );
        createAccountIfNotExists(
                "owner@travelmate.vn",
                "Owner@123",
                "Owner FullName",
                Set.of("OWNER", "CUSTOMER")
        );
        createAccountIfNotExists(
                "customer@travelmate.vn",
                "Customer@123",
                "Customer FullName",
                Set.of("CUSTOMER")
        );
    }

    private void createAccountIfNotExists(String email, String rawPassword, String fullName, Set<String> roleNames) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setIsEmailVerified(true); // Đã xác minh để test
        user.setStatus("ACTIVE"); // Active để test
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Set<Role> roles = roleRepository.findAll().stream()
                .filter(role -> roleNames.contains(role.getRoleName()))
                .collect(java.util.stream.Collectors.toSet());
        user.setRoles(roles);

        userRepository.save(user);
        System.out.println("✅ Created default user: " + email);
    }

    // --- HÀM MỚI: KHỞI TẠO AMENITIES ---
    private void initAmenities() {
        // 1. Danh sách tiện nghi CƠ SỞ LƯU TRÚ (Khớp với Frontend Step2_Amenities.jsx)
        List<String> propertyAmenities = List.of(
                "pool", "parking", "sauna", "spa", "non_smoking",
                "wifi", "airport_transfer", "pets", "gym",
                "smoking_area", "reception_24h", "ac"
        );

        for (String amenityName : propertyAmenities) {
            if (!amenityRepository.existsByAmenityNameAndAmenityType(amenityName, AmenityType.PROPERTY)) {
                Amenity amenity = new Amenity();
                amenity.setAmenityName(amenityName);
                amenity.setAmenityType(AmenityType.PROPERTY);
                amenity.setActive(true);
                amenityRepository.save(amenity);
                System.out.println("✅ Created Property Amenity: " + amenityName);
            }
        }

        // 2. Danh sách tiện nghi PHÒNG (Khớp với Frontend roomData.jsx)
        List<String> roomAmenities = List.of(
                "tv", "ac", "minibar", "tea_coffee", "wifi", "bathtub", "balcony", "non_smoking"
        );

        for (String amenityName : roomAmenities) {
            if (!amenityRepository.existsByAmenityNameAndAmenityType(amenityName, AmenityType.ROOM)) {
                Amenity amenity = new Amenity();
                amenity.setAmenityName(amenityName);
                amenity.setAmenityType(AmenityType.ROOM);
                amenity.setActive(true);
                amenityRepository.save(amenity);
                System.out.println("✅ Created Room Amenity: " + amenityName);
            }
        }
    }
}