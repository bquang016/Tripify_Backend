package com.example.smart_booking_system.service.impl;


import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import com.example.smart_booking_system.dto.PropertyResponseDTO;
import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.dto.response.property.PropertyMapDTO;
import com.example.smart_booking_system.dto.request.property.PropertyRegistrationRequest;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*; // AmenityType, PropertyStatus, PropertyType, RoomCategory
import com.example.smart_booking_system.exception.ForbiddenException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.*;
import com.example.smart_booking_system.util.SystemLogJsonUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final PropertyDetailRepository propertyDetailRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageService fileStorageService;
    private final RoomImageRepository roomImageRepository;
    private final BookingRepository bookingRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomRepository roomRepository;
    private final SystemLogService systemLogService;


    // ============================================================
    // VALIDATION
    // ============================================================
    private void validateProperty(Property property) {
        if (property.getAddress() == null || property.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Address cannot be empty");
    }

    // ============================================================
    // ADD PROPERTY
    // ============================================================

    @Override
    @Transactional
    public PropertyDetailDTO registerProperty(PropertyRegistrationRequest request, String ownerId) {

        // 1. Tìm Owner
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản chủ sở hữu"));

        // 2. Map DTO sang Entity
        Property property = new Property();
        property.setPropertyName(request.getPropertyName());
        property.setDescription(request.getDescription());
        property.setPropertyType(request.getPropertyType());
        property.setStarRating(request.getStarRating()); // Lưu hạng sao
        property.setAddress(request.getAddress());
        property.setCity(request.getCity());
        property.setDistrict(request.getDistrict());
        property.setWard(request.getWard());

        if (request.getLatitude() != null) property.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) property.setLongitude(BigDecimal.valueOf(request.getLongitude()));

        property.setOwner(owner);
        property.setActive(false); // Chờ Admin duyệt hoặc hoàn tất các bước sau
        property.setPropertyStatus(PropertyStatus.PENDING);

        // 3. Lưu Property trước để lấy ID
        Property savedProperty = propertyRepository.save(property);

        // 4. Xử lý Tiện ích (Amenities)
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            List<Amenity> selectedAmenities = amenityRepository.findAllById(request.getAmenityIds());

            List<PropertyAmenity> propertyAmenities = selectedAmenities.stream().map(amenity -> {
                PropertyAmenity pa = new PropertyAmenity();
                pa.setProperty(savedProperty);
                pa.setAmenity(amenity);
                return pa;
            }).toList();

            propertyAmenityRepository.saveAll(propertyAmenities);
            savedProperty.setPropertyAmenities(propertyAmenities);
        }

        // 5. Convert sang DTO để trả về (bạn có thể dùng Mapper hoặc build thủ công)
        return mapToPropertyDetailDTO(savedProperty);
    }
    @Override
    public Property addProperty(Property property, String ownerId) {

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found: " + ownerId));

        property.setOwner(owner);
        validateProperty(property);

        if (property.getPropertyStatus() == null) {
            property.setPropertyStatus(PropertyStatus.PENDING);
        }

        Property savedProperty = propertyRepository.save(property);

        systemLogService.log(
                owner,
                LogAction.CREATE,
                LogEntityType.PROPERTY,
                String.valueOf(savedProperty.getPropertyId()),
                "Tạo cơ sở lưu trú: " + savedProperty.getPropertyName(),
                null,
                null
        );

        try {
            sendPropertySubmittedEmail(owner, savedProperty);
        } catch (Exception ignored) {}

        return savedProperty;
    }

    // ============================================================
    // UPDATE PROPERTY
    // ============================================================
    @Override
    public PropertyDetailDTO updateProperty(int id, Property updatedProperty) {

        Property existing = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + id));

        // lưu old value
        String oldValue = existing.toString();

        if (updatedProperty.getPropertyName() != null)
            existing.setPropertyName(updatedProperty.getPropertyName());

        if (updatedProperty.getAddress() != null)
            existing.setAddress(updatedProperty.getAddress());

        if (updatedProperty.getCity() != null)
            existing.setDistrict(updatedProperty.getCity());

        if (updatedProperty.getProvince() != null) {
            existing.setCity(updatedProperty.getProvince());
            existing.setProvince(updatedProperty.getProvince());
        }

        if (updatedProperty.getCountry() != null)
            existing.setCountry(updatedProperty.getCountry());

        if (updatedProperty.getWard() != null)
            existing.setWard(updatedProperty.getWard());

        if (updatedProperty.getProvinceCode() != null)
            existing.setProvinceCode(updatedProperty.getProvinceCode());

        if (updatedProperty.getDistrictCode() != null)
            existing.setDistrictCode(updatedProperty.getDistrictCode());

        if (updatedProperty.getLatitude() != null)
            existing.setLatitude(updatedProperty.getLatitude());

        if (updatedProperty.getLongitude() != null)
            existing.setLongitude(updatedProperty.getLongitude());

        if (updatedProperty.getDescription() != null)
            existing.setDescription(updatedProperty.getDescription());

        existing.setUpdatedAt(LocalDate.now());

        Property saved = propertyRepository.save(existing);

        systemLogService.log(
                saved.getOwner(),
                LogAction.UPDATE,
                LogEntityType.PROPERTY,
                String.valueOf(saved.getPropertyId()),
                "Cập nhật thông tin cơ sở: " + saved.getPropertyName(),
                oldValue,
                SystemLogJsonUtil.propertySnapshot(saved)
        );

        return mapToPropertyDetailDTO(saved);
    }


    // ============================================================
    // 1. SUBMIT APPLICATION
    // ============================================================
    @Override
    public PropertyDetailDTO submitPropertyApplication(PropertyApplicationSubmitDTO dto,
                                                       List<MultipartFile> images,
                                                       String ownerId) {

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));

        Property property = new Property();
        property.setOwner(owner);
        property.setPropertyName(dto.getPropertyName());
        property.setDescription(dto.getDescription());
        property.setPropertyType(dto.getPropertyType());
        property.setAddress(dto.getAddress());
        property.setCity(dto.getProvince());
        property.setProvince(dto.getProvince());
        property.setCountry(dto.getCountry());
        property.setDistrict(dto.getCity());
        property.setWard(dto.getWard());
        property.setProvinceCode(dto.getProvinceCode());
        property.setDistrictCode(dto.getDistrictCode());

        property.setPhoneContact(owner.getPhoneNumber());
        property.setEmailContact(owner.getEmail());
        property.setPostalCode("70000");

        property.setLatitude(dto.getLatitude() != null ? dto.getLatitude() : BigDecimal.ZERO);
        property.setLongitude(dto.getLongitude() != null ? dto.getLongitude() : BigDecimal.ZERO);

        property.setPropertyStatus(PropertyStatus.PENDING);
        property.setActive(false);

        property.setCreatedAt(LocalDate.now());
        property.setUpdatedAt(LocalDate.now());

        Property saved = propertyRepository.save(property);

        // DETAIL
        PropertyDetail detail = new PropertyDetail();
        detail.setProperty(saved);
        detail.setArea(dto.getArea());
        propertyDetailRepository.save(detail);
        saved.setPropertyDetail(detail);

        // AMENITIES
        if (dto.getAmenities() != null) {
            dto.getAmenities().forEach((name, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) {
                    Amenity am = amenityRepository
                            .findByAmenityNameAndAmenityType(name, AmenityType.PROPERTY)
                            .orElse(null);
                    if (am != null) {
                        PropertyAmenity pa = new PropertyAmenity();
                        pa.setProperty(saved);
                        pa.setAmenity(am);
                        propertyAmenityRepository.save(pa);
                    }
                }
            });
        }

        // IMAGES
        if (images != null && !images.isEmpty()) {
            String folder = "properties/" + saved.getPropertyId();

            for (int i = 0; i < images.size(); i++) {
                String key = fileStorageService.storeImageFile(images.get(i), folder);

                PropertyImage img = new PropertyImage();
                img.setProperty(saved);
                img.setImageUrl(key);
                img.setCover(i == 0);

                propertyImageRepository.save(img);
            }
        }

        // SYSTEM LOG (SUBMIT APPLICATION)
        systemLogService.log(
                owner,
                LogAction.CREATE,
                LogEntityType.PROPERTY,
                String.valueOf(saved.getPropertyId()),
                "Gửi đơn đăng ký cơ sở lưu trú: " + saved.getPropertyName(),
                null,
                SystemLogJsonUtil.propertySnapshot(saved)
        );

        try {
            sendPropertySubmittedEmail(owner, saved);
        } catch (Exception ignored) {}

        // [BỔ SUNG] Gửi thông báo cho TẤT CẢ Admin
        try {
            notificationService.sendToAllAdmins(
                    "Cơ sở lưu trú mới chờ duyệt",
                    "Owner " + owner.getFullName() + " vừa đăng tải: " + saved.getPropertyName(),
                    NotificationType.ADMIN_NEW_PROPERTY_SUBMISSION,
                    String.valueOf(saved.getPropertyId())
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo Admin: " + e.getMessage());
        }

        // TỰ ĐỘNG TẠO ROOM NẾU LÀ HOMESTAY HOẶC VILLA
        if (dto.getPropertyType() == PropertyType.HOMESTAY || dto.getPropertyType() == PropertyType.VILLA) {

            Room room = new Room();
            room.setProperty(saved); // Liên kết với Property vừa tạo

            // Dùng tên căn user nhập, hoặc default nếu null
            room.setRoomName(dto.getUnitName() != null ? dto.getUnitName() : "Nguyên căn");

            // Set giá & sức chứa từ DTO
            room.setPricePerNight(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
            room.setWeekendPrice(dto.getWeekendPrice()); // Lưu giá cuối tuần
            room.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 2);

            room.setArea(dto.getArea()); // Lấy diện tích của Property gán cho Room luôn
            room.setDescription(dto.getDescription());

            // ✅ [FIX] THÊM DÒNG NÀY: Gán loại phòng là WHOLE để API Booking tìm thấy
            room.setRoomCategory(RoomCategory.WHOLE);

            // Mặc định room active false (chờ admin duyệt property thì room mới hiện)
            room.setActive(true); // Hoặc để false tuỳ logic duyệt
            room.setRoomAmount(1); // Nguyên căn thì số lượng là 1
            room.setCreatedDate(LocalDate.now());

            Room savedRoom = roomRepository.save(room);

            // [QUAN TRỌNG] Copy ảnh của Property sang cho Room này luôn
            // (Vì Homestay/Villa thì ảnh nhà chính là ảnh phòng)
            if (images != null && !images.isEmpty()) {
                // Logic copy hoặc tạo record RoomImage trỏ cùng url với PropertyImage
                // Để đơn giản, ta tạo record RoomImage mới nhưng dùng chung URL (key) đã upload
                List<PropertyImage> propImages = propertyImageRepository.findByProperty_PropertyId(saved.getPropertyId());
                for (PropertyImage pImg : propImages) {
                    RoomImage rImg = new RoomImage();
                    rImg.setRoom(savedRoom);
                    rImg.setImageUrl(pImg.getImageUrl()); // Dùng lại key ảnh S3/R2 cũ, ko cần upload lại
                    roomImageRepository.save(rImg);
                }
            }
        }


        return mapToPropertyDetailDTO(saved);
    }


    @Override
    public boolean checkNameAvailability(String propertyName) {
        return !propertyRepository.existsByPropertyName(propertyName);
    }

    // ============================================================
    // GET PROPERTY DETAIL (SIGNED URL)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public PropertyDetailDTO getPropertyDetailById(Integer id, LocalDate checkIn, LocalDate checkOut) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        PropertyDetailDTO dto = mapToPropertyDetailDTO(property);

        // ALL IMAGES signed
        List<String> signedImages = propertyImageRepository.findByProperty_PropertyId(id)
                .stream()
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .collect(Collectors.toList());

        dto.setImages(signedImages);

        // AMENITIES
        if (property.getPropertyAmenities() != null) {
            dto.setAmenities(
                    property.getPropertyAmenities().stream()
                            .filter(PropertyAmenity::isActive)
                            .map(PropertyAmenityResponseDTO::new)
                            .collect(Collectors.toList())
            );
        }

        // ROOMS
        List<RoomResponseDTO> rooms = new ArrayList<>();

        if (property.getRooms() != null) {
            for (Room room : property.getRooms()) {
                if (!room.isActive()) continue;

                if (checkIn != null && checkOut != null) {
                    boolean booked = !bookingRepository
                            .findConfirmedOverlappingByRoomId(room.getRoomId(), checkIn, checkOut)
                            .isEmpty();
                    if (booked) continue;
                }

                RoomResponseDTO rDto = new RoomResponseDTO(room);

                List<String> roomImages = roomImageRepository.findByRoom_RoomId(room.getRoomId())
                        .stream()
                        .map(RoomImage::getImageUrl)
                        .map(fileStorageService::generateSignedUrl)
                        .collect(Collectors.toList());
                rDto.setImages(roomImages);

                List<String> ams = roomAmenityRepository.findByRoom_RoomId(room.getRoomId())
                        .stream()
                        .filter(RoomAmenity::isActive)
                        .map(ra -> ra.getAmenity().getAmenityName())
                        .collect(Collectors.toList());
                rDto.setAmenities(ams);

                rooms.add(rDto);
            }
        }

        dto.setRooms(rooms);

        return dto;
    }

    // ============================================================
    // SEARCH (list view → PropertyResponseDTO)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> searchProperties(String keyword,
                                                    Integer guests,
                                                    LocalDate checkIn,
                                                    LocalDate checkOut) {

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        List<Property> properties = propertyRepository.searchProperties(keyword, guests, checkIn, checkOut);

        return properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OWNER PROPERTIES (list → use PropertyDetailDTO)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getOwnerProperties(String ownerId) {
        return propertyRepository.findAllByOwnerIdAndNotRejected(ownerId)
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getOwnerActiveProperties(String ownerId) {
        return propertyRepository.findByOwner_UserIdAndPropertyStatus(ownerId, PropertyStatus.APPROVE)
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // FEATURED (list → use PropertyResponseDTO)
    // ============================================================
    @Override
    public List<PropertyDetailDTO> getFeaturedProperties() {
        return propertyRepository.findFeaturedProperties()
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // STATUS FILTER (ADMIN LIST → NEED SIGNED URL COVER)
    // ============================================================
    @Override
    public List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status) {
        return propertyRepository.findByPropertyStatus(status)
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // REVIEW
    // ============================================================
    @Override
    public PropertyDetailDTO reviewProperty(Integer propertyId,
                                            PropertyReviewDTO reviewDTO,
                                            String adminUsername) {

        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getPropertyStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Property already reviewed");
        }

        if (!reviewDTO.isValidForReview()) {
            throw new IllegalArgumentException("Chỉ được APPROVE hoặc REJECTED.");
        }

        String oldValue = property.getPropertyStatus().name();

        PropertyStatus newStatus = reviewDTO.getStatus();

        property.setPropertyStatus(newStatus);
        property.setUpdatedAt(LocalDate.now());
        property.setActive(newStatus == PropertyStatus.APPROVE);

        Property saved = propertyRepository.save(property);

        // SYSTEM LOG (ADMIN REVIEW)
        systemLogService.log(
                admin,
                LogAction.UPDATE,
                LogEntityType.PROPERTY,
                String.valueOf(saved.getPropertyId()),
                newStatus == PropertyStatus.APPROVE
                        ? "Admin duyệt cơ sở lưu trú: " + saved.getPropertyName()
                        : "Admin từ chối cơ sở lưu trú: " + saved.getPropertyName(),
                oldValue,
                newStatus.name()
        );

        // [BỔ SUNG] Gửi thông báo + Email cho Owner
        if (property.getOwner() != null) {
            User owner = property.getOwner();

            // 1. Gửi Email (Code cũ)
            sendPropertyReviewEmail(owner, saved, reviewDTO.getReason());

            // 2. Gửi Notification In-App (Code mới)
            try {
                if (newStatus == PropertyStatus.APPROVE) {
                    notificationService.sendNotification(
                            owner.getUserId(),
                            "Cơ sở lưu trú được chấp thuận",
                            "Chúc mừng! Cơ sở '" + saved.getPropertyName() + "' đã được duyệt và đang hoạt động trên hệ thống.",
                            NotificationType.APPROVAL,
                            String.valueOf(saved.getPropertyId())
                    );
                } else if (newStatus == PropertyStatus.REJECTED) {
                    notificationService.sendNotification(
                            owner.getUserId(),
                            "Cơ sở lưu trú bị từ chối",
                            "Cơ sở '" + saved.getPropertyName() + "' không đạt yêu cầu. Lý do: " +
                                    (reviewDTO.getReason() != null ? reviewDTO.getReason() : "Không rõ lý do"),
                            NotificationType.REJECTION,
                            String.valueOf(saved.getPropertyId())
                    );
                }
            } catch (Exception e) {
                System.err.println("Lỗi gửi thông báo Owner: " + e.getMessage());
            }
        }

        return mapToPropertyDetailDTO(saved);
    }


    // ============================================================
    // MAPPER: DETAIL DTO (cover + images signed)
    // ============================================================
    private PropertyDetailDTO mapToPropertyDetailDTO(Property property) {

        PropertyDetailDTO dto = new PropertyDetailDTO(property);

        // cover image signed
        propertyImageRepository.findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                .ifPresentOrElse(
                        img -> dto.setCoverImage(fileStorageService.generateSignedUrl(img.getImageUrl())),
                        () -> propertyImageRepository.findFirstByProperty_PropertyId(property.getPropertyId())
                                .ifPresent(img -> dto.setCoverImage(fileStorageService.generateSignedUrl(img.getImageUrl())))
                );

        // list images signed
        List<String> signed = propertyImageRepository.findByProperty_PropertyId(property.getPropertyId())
                .stream()
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .collect(Collectors.toList());
        dto.setImages(signed);

        // price range
        if (property.getRooms() != null && !property.getRooms().isEmpty()) {
            List<BigDecimal> prices = property.getRooms().stream()
                    .filter(Room::isActive) // Chỉ tính phòng đang hoạt động
                    .map(Room::getPricePerNight)
                    .toList();

            if (!prices.isEmpty()) {
                dto.setMinPrice(prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                dto.setMaxPrice(prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            }
        }

        return dto;
    }

    // ============================================================
    // MAPPER: LIST VIEW → PropertyResponseDTO (SIGNED COVER)
    // ============================================================
    private PropertyResponseDTO mapToPropertyResponseDTO(Property property) {

        PropertyResponseDTO dto = new PropertyResponseDTO();

        dto.setPropertyId(property.getPropertyId());
        dto.setPropertyName(property.getPropertyName());
        dto.setPropertyType(property.getPropertyType());
        dto.setAddress(property.getAddress());
        dto.setCountry(property.getCountry());
        dto.setProvince(property.getProvince());
        dto.setCity(property.getCity());
        dto.setWard(property.getWard());
        dto.setProvinceCode(property.getProvinceCode());
        dto.setDistrictCode(property.getDistrictCode());
        dto.setPostalCode(property.getPostalCode());
        dto.setDescription(property.getDescription());
        dto.setPhoneContact(property.getPhoneContact());
        dto.setEmailContact(property.getEmailContact());
        dto.setLatitude(property.getLatitude());
        dto.setLongitude(property.getLongitude());
        dto.setCreatedAt(property.getCreatedAt());
        dto.setUpdatedAt(property.getUpdatedAt());
        dto.setPropertyStatus(property.getPropertyStatus());
        dto.setActive(property.isActive());

        if (property.getOwner() != null) {
            dto.setOwnerName(property.getOwner().getFullName());
        }

        // Signed cover
        String cover = propertyImageRepository
                .findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .orElse(null);

        dto.setCoverImage(cover);

        List<String> allImages = propertyImageRepository.findByProperty_PropertyId(property.getPropertyId())
                .stream()
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .collect(Collectors.toList());

        dto.setImages(allImages); // Gán vào DTO

        return dto;
    }

    // ============================================================
    // NEARBY (signed cover)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<PropertyMapDTO> findNearbyProperties(Double lat, Double lng, Double radius) {

        if (lat == null || lng == null) return new ArrayList<>();

        double range = radius != null ? radius : 10.0;

        return propertyRepository.findNearbyProperties(lat, lng, range)
                .stream()
                .map(property -> {

                    String cover = propertyImageRepository
                            .findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                            .map(PropertyImage::getImageUrl)
                            .map(fileStorageService::generateSignedUrl)
                            .orElse(null);

                    BigDecimal minPrice = property.getRooms() != null
                            ? property.getRooms().stream()
                            .map(Room::getPricePerNight)
                            .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO)
                            : BigDecimal.ZERO;

                    return new PropertyMapDTO(property, cover, minPrice);
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // EMAIL HELPERS
    // ============================================================
    private void sendPropertySubmittedEmail(User owner, Property property) {
        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());

        emailService.sendHtmlEmail(
                owner.getEmail(),
                "Xác nhận nộp đơn",
                "email/property-submitted-confirmation",
                context
        );
    }

    private void sendPropertyReviewEmail(User owner, Property property, String reason) {
        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());
        context.setVariable("reason", reason != null ? reason : "N/A");

        String template = property.getPropertyStatus() == PropertyStatus.APPROVE
                ? "email/property-approved"
                : "email/property-rejected";

        String subject = property.getPropertyStatus() == PropertyStatus.APPROVE
                ? "Cơ sở được duyệt"
                : "Cơ sở bị từ chối";

        emailService.sendHtmlEmail(owner.getEmail(), subject, template, context);
    }

    @Override
    public boolean togglePropertyStatus(Integer propertyId, String ownerId) {
        // 1. Tìm Property theo ID
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở lưu trú"));

        // 2. Kiểm tra quyền sở hữu
        if (!property.getOwner().getUserId().equals(ownerId)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa cơ sở này");
        }

        // 3. Kiểm tra trạng thái duyệt
        if (property.getPropertyStatus() != PropertyStatus.APPROVE) {
            throw new IllegalArgumentException(
                    "Cơ sở phải được Admin duyệt (APPROVE) mới có thể thay đổi trạng thái hoạt động."
            );
        }

        boolean oldStatus = property.isActive();

        // 4. Đảo ngược trạng thái active
        boolean newStatus = !oldStatus;
        property.setActive(newStatus);
        property.setUpdatedAt(LocalDate.now());

        Property saved = propertyRepository.save(property);

        // ✅ SYSTEM LOG (OWNER TOGGLE ACTIVE)
        systemLogService.log(
                saved.getOwner(),
                LogAction.UPDATE,
                LogEntityType.PROPERTY,
                String.valueOf(saved.getPropertyId()),
                newStatus
                        ? "Chủ sở hữu bật hoạt động cơ sở: " + saved.getPropertyName()
                        : "Chủ sở hữu tắt hoạt động cơ sở: " + saved.getPropertyName(),
                String.valueOf(oldStatus),
                String.valueOf(newStatus)
        );

        return newStatus;
    }

    // ============================================================
    // [NEW] SUSPEND PROPERTY (ADMIN)
    // ============================================================
    @Override
    public void suspendProperty(Integer propertyId, String reason) {
        // 1. Tìm Property
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy cơ sở lưu trú với ID: " + propertyId
                ));

        String oldStatus = property.getPropertyStatus().name();

        // 2. Cập nhật trạng thái
        property.setPropertyStatus(PropertyStatus.SUSPENDED);
        property.setActive(false);
        property.setUpdatedAt(LocalDate.now());

        Property saved = propertyRepository.save(property);

        // 3. Lấy chủ sở hữu
        User owner = saved.getOwner();

        // ✅ SYSTEM LOG (ADMIN SUSPEND)
        systemLogService.log(
                owner,
                LogAction.UPDATE,
                LogEntityType.PROPERTY,
                String.valueOf(saved.getPropertyId()),
                "Admin tạm dừng cơ sở lưu trú: " + saved.getPropertyName(),
                oldStatus,
                PropertyStatus.SUSPENDED.name()
        );

        // 4. Gửi Notification
        notificationService.sendNotification(
                owner.getUserId(),
                "Cơ sở lưu trú bị tạm dừng hoạt động",
                "Cơ sở '" + saved.getPropertyName() + "' đã bị admin tạm dừng. Lý do: " + reason,
                NotificationType.PROPERTY_SUSPENDED,
                String.valueOf(saved.getPropertyId())
        );

        // 5. Gửi Email
        emailService.sendPropertySuspensionEmail(
                owner.getEmail(),
                owner.getFullName(),
                saved.getPropertyName(),
                reason
        );
    }


    // ============================================================
    // [NEW] GET ALL ACTIVE PROPERTIES (ADMIN)
    // ============================================================
    @Override
    public Page<PropertyResponseDTO> getAllActiveProperties(Pageable pageable) {
        // ✅ FIX LỖI: Repository đã có hàm nhận Pageable
        return propertyRepository.findByPropertyStatus(PropertyStatus.APPROVE, pageable)
                .map(this::mapToPropertyResponseDTO);
    }
    @Override
    public void activateProperty(Integer propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        String oldStatus = property.getPropertyStatus().name();

        // Khôi phục trạng thái
        property.setPropertyStatus(PropertyStatus.APPROVE);
        property.setActive(true);
        property.setUpdatedAt(LocalDate.now());

        Property saved = propertyRepository.save(property);

        User owner = saved.getOwner();

        // SYSTEM LOG (ADMIN ACTIVATE)
        systemLogService.log(
                owner,
                LogAction.UPDATE,
                LogEntityType.PROPERTY,
                String.valueOf(saved.getPropertyId()),
                "Admin kích hoạt lại cơ sở lưu trú: " + saved.getPropertyName(),
                oldStatus,
                PropertyStatus.APPROVE.name()
        );

        // Gửi thông báo
        notificationService.sendNotification(
                owner.getUserId(),
                "Cơ sở hoạt động trở lại",
                "Cơ sở " + saved.getPropertyName() + " đã được mở lại.",
                NotificationType.SYSTEM,
                String.valueOf(saved.getPropertyId())
        );

        // Gửi mail
        emailService.sendPropertyReactivationEmail(
                owner.getEmail(),
                owner.getFullName(),
                saved.getPropertyName()
        );
    }

    @Override
    public Page<PropertyResponseDTO> getPropertiesByStatusPaginated(PropertyStatus status, Pageable pageable) {
        return propertyRepository.findByPropertyStatus(status, pageable)
                .map(this::mapToPropertyResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyDetailDTO> searchPropertiesPaginated(
            String keyword,
            List<String> cities,
            List<Integer> ratings,
            Integer guests,
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean isManager, // <--- Nhận tham số từ Controller
            Pageable pageable) {

        List<BookingStatus> bookingStatuses = Arrays.asList(
                BookingStatus.CONFIRMED,
                BookingStatus.PENDING_PAYMENT
        );

        // 1. Gọi Repository (Truyền isManager xuống SQL)
        Page<Property> propertyPage = propertyRepository.searchPropertiesAdvanced(
                PropertyStatus.APPROVE,
                keyword,
                cities,
                ratings,
                guests,
                checkIn,
                checkOut,
                minPrice,
                maxPrice,
                bookingStatuses,
                isManager, // <--- Truyền vào đây
                pageable
        );

        // 2. Map & Filter Rooms (Logic hiển thị)
        return propertyPage.map(property -> {
            PropertyDetailDTO dto = mapToPropertyDetailDTO(property);

            if (dto.getRooms() != null) {
                List<RoomResponseDTO> filteredRooms = dto.getRooms().stream()
                        .filter(room -> {
                            // [QUAN TRỌNG] Nếu là Manager thì HIỂN THỊ HẾT (kể cả active=false)
                            if (isManager) return true;

                            // --- Logic cho Khách (Customer) ---

                            // Phải là phòng đang active
                            // Note: DTO của bạn cần có field isActive, hoặc check logic khác.
                            // Nếu RoomResponseDTO chưa có isActive, tạm thời bỏ qua dòng này hoặc bổ sung vào DTO.
                            // if (!room.isActive()) return false;

                            // Check giá & sức chứa
                            if (minPrice != null && room.getPricePerNight().compareTo(minPrice) < 0) return false;
                            if (maxPrice != null && room.getPricePerNight().compareTo(maxPrice) > 0) return false;
                            if (guests != null && room.getCapacity() < guests) return false;

                            // Check trùng lịch
                            if (checkIn != null && checkOut != null) {
                                Long bookedCount = bookingRepository.countExistingBookings(room.getRoomId(), checkIn, checkOut);
                                if (bookedCount > 0) return false;
                            }

                            return true;
                        })
                        .collect(Collectors.toList());

                dto.setRooms(filteredRooms);

                // Update lại min/max price hiển thị ngoài thẻ
                if (!filteredRooms.isEmpty()) {
                    dto.setMinPrice(filteredRooms.stream()
                            .map(RoomResponseDTO::getPricePerNight)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO));

                    dto.setMaxPrice(filteredRooms.stream()
                            .map(RoomResponseDTO::getPricePerNight)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO));
                } else {
                    // Nếu sau khi lọc không còn phòng nào (trường hợp hiếm do SQL đã lọc rồi, nhưng vẫn có thể xảy ra do logic fetch Eager)
                    // Ta có thể để giá = 0 hoặc null
                    dto.setMinPrice(BigDecimal.ZERO);
                    dto.setMaxPrice(BigDecimal.ZERO);
                }
            }
            return dto;
        });
    }
}
