package com.example.smart_booking_system.config;

import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.*;
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
    private final PermissionRepository permissionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @PostConstruct
    public void init() {
        // 1. Khởi tạo Permissions
        initPermissions();

        // 2. Khởi tạo Roles
        initRoles();

        // 3. Khởi tạo Users
        initDefaultUsers();

        // 4. Khởi tạo Amenities (THÊM MỚI)
        initAmenities();

        // 5. Khởi tạo System Settings (THÊM MỚI)
        initSystemSettings();
    }

    private void initSystemSettings() {
        if (!systemSettingRepository.existsById(1L)) {
            SystemSetting settings = new SystemSetting();
            settings.setId(1L);
            settings.setAppName("Tripify");
            settings.setDefaultLanguage("vi");
            settings.setDefaultCurrency("VND");
            systemSettingRepository.save(settings);
            System.out.println("✅ Initialized default System Settings");
        }

        if (exchangeRateRepository.count() == 0) {
            ExchangeRate usd = new ExchangeRate();
            usd.setPair("USD_VND");
            usd.setRate(25450.0);
            exchangeRateRepository.save(usd);

            ExchangeRate eur = new ExchangeRate();
            eur.setPair("EUR_VND");
            eur.setRate(27120.0);
            exchangeRateRepository.save(eur);
            System.out.println("✅ Initialized default Exchange Rates");
        }
    }

    private void initPermissions() {
        // Lấy tất cả permission hiện có để check in-memory, tránh crash nếu cột code chưa tồn tại trong DB
        // Tuy nhiên, nếu cột 'code' chưa tồn tại, query này vẫn có thể fail nếu JPA query mặc định dùng code.
        // Giải pháp: Dùng native query hoặc check an toàn.
        
        try {
            // Group: User Management
            createPermissionIfNotExist("USER_VIEW", "Xem danh sách người dùng", "Quản lý người dùng");
            createPermissionIfNotExist("USER_CREATE", "Tạo mới người dùng", "Quản lý người dùng");
            createPermissionIfNotExist("USER_UPDATE", "Cập nhật người dùng", "Quản lý người dùng");
            createPermissionIfNotExist("USER_DELETE", "Xóa người dùng", "Quản lý người dùng");

            // Group: Property Management
            createPermissionIfNotExist("PROPERTY_VIEW", "Xem danh sách cơ sở lưu trú", "Quản lý khách sạn");
            createPermissionIfNotExist("PROPERTY_APPROVE", "Phê duyệt cơ sở lưu trú", "Quản lý khách sạn");
            createPermissionIfNotExist("PROPERTY_MANAGE", "Quản lý cơ sở lưu trú", "Quản lý khách sạn");
            createPermissionIfNotExist("APPLICATION_MANAGE", "Quản lý đơn đăng ký đối tác", "Quản lý khách sạn");

            // Group: Booking Management
            createPermissionIfNotExist("BOOKING_VIEW", "Xem danh sách đặt phòng", "Quản lý đặt phòng");
            createPermissionIfNotExist("BOOKING_MANAGE", "Quản lý đặt phòng", "Quản lý đặt phòng");

            // Group: Promotion Management
            createPermissionIfNotExist("PROMOTION_VIEW", "Xem danh sách khuyến mãi", "Quản lý khuyến mãi");
            createPermissionIfNotExist("PROMOTION_MANAGE", "Quản lý khuyến mãi", "Quản lý khuyến mãi");

            // Group: System
            createPermissionIfNotExist("SYSTEM_LOG_VIEW", "Xem log hệ thống", "Hệ thống");
            createPermissionIfNotExist("SYSTEM_MANAGE", "Quản lý cấu hình hệ thống", "Hệ thống");
            createPermissionIfNotExist("REPORTS_VIEW", "Xem báo cáo doanh thu", "Hệ thống");
            createPermissionIfNotExist("PAYMENT_APPROVE", "Phê duyệt thanh toán", "Hệ thống");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not initialize permissions. This is likely because the database schema is being updated. Please restart the application. Error: " + e.getMessage());
        }
    }

    private void createPermissionIfNotExist(String code, String name, String groupName) {
        boolean exists = false;
        try {
            exists = permissionRepository.existsByCode(code);
        } catch (Exception e) {
            // Nếu cột code chưa tồn tại, thử check theo name
            try {
                exists = permissionRepository.existsByName(name);
            } catch (Exception e2) {
                exists = false;
            }
        }

        if (!exists) {
            Permission p = new Permission();
            p.setCode(code);
            p.setName(name);
            p.setGroupName(groupName);
            p.setDescription("Quyền " + name);
            p.setCreatedAt(LocalDateTime.now());
            permissionRepository.save(p);
            System.out.println("✅ Created permission: " + code);
        }
    }

    private void initRoles() {
        // 1. Super Admin
        Role superAdmin = roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("SUPER_ADMIN");
                    role.setDescription("Vai trò tối cao, toàn quyền hệ thống");
                    role.setIsSuper(true);
                    role.setCreatedAt(LocalDateTime.now());
                    Role saved = roleRepository.save(role);
                    System.out.println("✅ Created role: SUPER_ADMIN");
                    return saved;
                });
        
        // Super Admin luôn được cập nhật full quyền để đảm bảo không bị lock-out
        Set<Permission> allPerms = new java.util.HashSet<>(permissionRepository.findAll());
        if (superAdmin.getPermissions().size() != allPerms.size()) {
            superAdmin.setPermissions(allPerms);
            roleRepository.save(superAdmin);
            System.out.println("✅ Synchronized full permissions for SUPER_ADMIN");
        }

        // 2. Admin thường
        roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    role.setDescription("Quản trị viên hệ thống");
                    role.setIsSuper(false);
                    role.setCreatedAt(LocalDateTime.now());
                    
                    // Chỉ gán quyền mặc định khi TẠO MỚI lần đầu
                    Set<Permission> adminPerms = permissionRepository.findAll().stream()
                            .filter(p -> !p.getCode().equals("SYSTEM_LOG_VIEW"))
                            .collect(java.util.stream.Collectors.toSet());
                    role.setPermissions(adminPerms);
                    
                    Role saved = roleRepository.save(role);
                    System.out.println("✅ Created role: ADMIN with default permissions");
                    return saved;
                });

        // 3. Hotel Owner
        roleRepository.findByName("OWNER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("OWNER");
                    role.setDescription("Chủ cơ sở lưu trú");
                    role.setIsSuper(false);
                    role.setCreatedAt(LocalDateTime.now());
                    Role saved = roleRepository.save(role);
                    System.out.println("✅ Created role: OWNER");
                    return saved;
                });

        // 4. Customer
        roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("CUSTOMER");
                    role.setDescription("Khách hàng");
                    role.setIsSuper(false);
                    role.setCreatedAt(LocalDateTime.now());
                    Role saved = roleRepository.save(role);
                    System.out.println("✅ Created role: CUSTOMER");
                    return saved;
                });
    }

    private void initDefaultUsers() {
        // Super Admin
        createAccountIfNotExists(
                "superadmin@travelmate.vn",
                "SuperAdmin@123",
                "Super Admin",
                Set.of("SUPER_ADMIN")
        );
        // Admin thường
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
                .filter(role -> roleNames.contains(role.getName()))
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