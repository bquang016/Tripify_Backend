package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import com.example.smart_booking_system.dto.response.PriceForecastDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.*;
import com.example.smart_booking_system.util.SystemLogJsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomImageRepository roomImageRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final CurrencyService currencyService;

    @Override
    public boolean checkRoomNameExists(int propertyId, String roomName, int excludeRoomId) {
        // ✅ SỬA: Tên hàm Repository mới (theo biến property)
        return roomRepository.existsByProperty_PropertyIdAndRoomNameAndIdNot(propertyId, roomName, excludeRoomId);
    }

    // ============================================================
    // ✅ [FIXED] LẤY DANH SÁCH PHÒNG (CHO ADMIN)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByPropertyId(int propertyId) {
        // ✅ SỬA: Tên hàm Repository mới (theo biến property và active)
        List<Room> rooms = roomRepository.findByProperty_PropertyIdAndActiveTrue(propertyId);
        // SỬA: Dùng hàm lấy tất cả phòng (cả Active và Suspended)
        // Lưu ý: Bạn cần đảm bảo RoomRepository đã có hàm findByPropertyId_PropertyId
        List<Room> room = roomRepository.findByProperty_PropertyId(propertyId);

        return rooms.stream().map(this::mapToRoomDTO).collect(Collectors.toList());
    }

    @Override
    public RoomResponseDTO addRoom(RoomRequestDTO dto, List<MultipartFile> images) {
        Property property = propertyRepository.findById(dto.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Room room = new Room();
        // ✅ SỬA: setProperty thay vì setPropertyId
        room.setProperty(property);
        room.setRoomName(dto.getRoomName());

        // roomCategory và roomStatus: Nếu Entity vẫn giữ thì để nguyên, nếu xóa thì xóa dòng này
        room.setRoomCategory(dto.getRoomCategory());
        room.setRoomStatus(RoomStatus.AVAILABLE);

        room.setPricePerNight(dto.getPricePerNight());

        // Logic weekendPrice
        if (dto.getWeekendPrice() != null && dto.getWeekendPrice().compareTo(BigDecimal.ZERO) > 0) {
            room.setWeekendPrice(dto.getWeekendPrice());
        } else {
            room.setWeekendPrice(dto.getPricePerNight());
        }

        // ✅ BỔ SUNG: Set các trường mới (Area, RoomAmount, CreatedDate)
        // Nếu DTO RoomRequestDTO chưa có các trường này, bạn cần thêm vào DTO hoặc set default
        // Ví dụ tạm thời set default hoặc lấy từ DTO nếu có
        // room.setArea(dto.getArea());
        // room.setRoomAmount(dto.getRoomAmount());
        room.setCreatedDate(LocalDate.now());

        room.setCapacity(dto.getCapacity());
        room.setDescription(dto.getDescription());

        // ✅ SỬA: setActive thay vì setIsActive (tùy lombok, thường setActive là chuẩn)
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);

        systemLogService.log(
                property.getOwner(),
                LogAction.CREATE,
                LogEntityType.ROOM,
                String.valueOf(savedRoom.getRoomId()),
                "Tạo phòng: " + savedRoom.getRoomName(),
                null,
                null
        );

        if (dto.getAmenities() != null) {
            for (String frontendId : dto.getAmenities()) {
                amenityRepository.findByAmenityNameAndAmenityType(frontendId, com.example.smart_booking_system.enums.AmenityType.ROOM)
                        .ifPresent(amenity -> {
                            RoomAmenity ra = new RoomAmenity();
                            ra.setRoom(savedRoom); // (Trong OwnerApplicationServiceImpl dùng savedUnit, RoomServiceImpl dùng savedRoom)
                            ra.setAmenity(amenity);
                            ra.setActive(true);
                            roomAmenityRepository.save(ra); // Hoặc roomAmenityRepository.save(ra)
                        });
            }
        }

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String path = fileStorageService.storeImageFile(file, "rooms");
                RoomImage roomImage = new RoomImage();
                roomImage.setRoom(savedRoom);
                roomImage.setImageUrl(path);
                roomImageRepository.save(roomImage);
            }
        }

        return mapToRoomDTO(savedRoom);
    }

    @Override
    public RoomResponseDTO updateRoom(int roomId, RoomRequestDTO dto, List<MultipartFile> newImages) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        String oldValue = SystemLogJsonUtil.roomSnapshot(room);

        room.setRoomName(dto.getRoomName());

        // roomCategory (nếu còn dùng)
        room.setRoomCategory(dto.getRoomCategory());

        room.setPricePerNight(dto.getPricePerNight());

        if (dto.getWeekendPrice() != null && dto.getWeekendPrice().compareTo(BigDecimal.ZERO) > 0) {
            room.setWeekendPrice(dto.getWeekendPrice());
        } else {
            room.setWeekendPrice(dto.getPricePerNight());
        }

        // ✅ Cập nhật thêm các trường mới nếu DTO có gửi lên
        // room.setArea(dto.getArea());
        // room.setRoomAmount(dto.getRoomAmount());

        room.setCapacity(dto.getCapacity());
        room.setDescription(dto.getDescription());

        Room savedRoom = roomRepository.save(room);

        // CẬP NHẬT TIỆN ÍCH PHÒNG
        if (dto.getAmenities() != null) {
            // 1. Xóa hết tiện ích cũ của phòng này để làm mới
            List<RoomAmenity> oldAmenities = roomAmenityRepository.findByRoom_RoomId(savedRoom.getRoomId());
            roomAmenityRepository.deleteAll(oldAmenities);

            // 2. Thêm lại danh sách tiện ích mới
            for (String frontendId : dto.getAmenities()) {
                amenityRepository.findByAmenityNameAndAmenityType(frontendId, com.example.smart_booking_system.enums.AmenityType.ROOM)
                        .ifPresent(amenity -> {
                            RoomAmenity ra = new RoomAmenity();
                            ra.setRoom(savedRoom); // (Trong OwnerApplicationServiceImpl dùng savedUnit, RoomServiceImpl dùng savedRoom)
                            ra.setAmenity(amenity);
                            ra.setActive(true);
                            roomAmenityRepository.save(ra); // Hoặc roomAmenityRepository.save(ra)
                        });
            }
        }

        systemLogService.log(
                room.getProperty().getOwner(),
                LogAction.UPDATE,
                LogEntityType.ROOM,
                String.valueOf(savedRoom.getRoomId()),
                "Cập nhật phòng: " + savedRoom.getRoomName(),
                oldValue,
                SystemLogJsonUtil.roomSnapshot(savedRoom)
        );

        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String path = fileStorageService.storeImageFile(file, "rooms");
                RoomImage roomImage = new RoomImage();
                roomImage.setRoom(room);
                roomImage.setImageUrl(path);
                roomImageRepository.save(roomImage);
            }
        }

        return mapToRoomDTO(savedRoom);
    }

    @Override
    public void deleteRoom(int roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        String oldValue = SystemLogJsonUtil.roomSnapshot(room);

        room.setActive(false);
        roomRepository.save(room);

        systemLogService.log(
                room.getProperty().getOwner(),
                LogAction.UPDATE,
                LogEntityType.ROOM,
                String.valueOf(room.getRoomId()),
                "Ẩn phòng: " + room.getRoomName(),
                oldValue,
                SystemLogJsonUtil.roomSnapshot(room)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDTO getRoomById(int roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        return mapToRoomDTO(room);
    }

    // Logic lấy giá theo ngày
    @Override
    @Transactional(readOnly = true)
    public List<PriceForecastDTO> getPriceForecast(int roomId, LocalDate startDate, int days) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<PriceForecastDTO> forecastList = new ArrayList<>();
        LocalDate current = (startDate != null) ? startDate : LocalDate.now();

        for (int i = 0; i < days; i++) {
            LocalDate date = current.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
            BigDecimal price = isWeekend ? room.getWeekendPrice() : room.getPricePerNight();
            forecastList.add(new PriceForecastDTO(date, dayOfWeek.name(), price, isWeekend));
        }
        return forecastList;
    }

// ============================================================
// SUSPEND ROOM
// ============================================================
@Override
public void suspendRoom(Integer roomId, String reason) {
    Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng với ID: " + roomId));

    String oldValue = SystemLogJsonUtil.roomSnapshot(room);

    room.setRoomStatus(RoomStatus.SUSPENDED);
    room.setActive(false);
    roomRepository.save(room);

    systemLogService.log(
            room.getProperty().getOwner(),
            LogAction.UPDATE,
            LogEntityType.ROOM,
            String.valueOf(room.getRoomId()),
            "Tạm dừng phòng: " + room.getRoomName() + " | Lý do: " + reason,
            oldValue,
            SystemLogJsonUtil.roomSnapshot(room)
    );

    Property property = room.getProperty();
    User owner = property.getOwner();

    String title = "Tạm dừng phòng tại " + property.getPropertyName();
    String message = "Phòng '" + room.getRoomName() + "' thuộc khách sạn '" +
            property.getPropertyName() + "' đã bị khóa. Lý do: " + reason;

    notificationService.sendNotification(
            owner.getUserId(),
            title,
            message,
            NotificationType.ROOM_SUSPENDED,
            String.valueOf(room.getRoomId())
    );

    emailService.sendRoomSuspensionEmail(
            owner.getEmail(),
            owner.getFullName(),
            property.getPropertyName(),
            room.getRoomName(),
            reason
    );
}

    @Override
    public List<RoomResponseDTO> getAllRoomsByPropertyId(Integer propertyId) {
        // Gọi Repository lấy tất cả phòng theo propertyId (không lọc active)
        List<Room> rooms = roomRepository.findAllByProperty_PropertyId(propertyId);

        // Convert sang DTO
        return rooms.stream()
                .map(this::mapToRoomDTO) // Sử dụng hàm mapper có sẵn
                .collect(Collectors.toList());
    }

// ============================================================
// ACTIVATE ROOM
// ============================================================
@Override
public void activateRoom(Integer roomId) {
    Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

    String oldValue = SystemLogJsonUtil.roomSnapshot(room);

    room.setRoomStatus(RoomStatus.AVAILABLE);
    room.setActive(true);
    roomRepository.save(room);

    systemLogService.log(
            room.getProperty().getOwner(),
            LogAction.UPDATE,
            LogEntityType.ROOM,
            String.valueOf(room.getRoomId()),
            "Mở lại phòng: " + room.getRoomName(),
            oldValue,
            SystemLogJsonUtil.roomSnapshot(room)
    );

    Property property = room.getProperty();
    User owner = property.getOwner();

    String title = "Phòng tại " + property.getPropertyName() + " hoạt động trở lại";
    String message = "Phòng '" + room.getRoomName() + "' thuộc khách sạn '" +
            property.getPropertyName() + "' đã được mở khóa và sẵn sàng nhận khách.";

    notificationService.sendNotification(
            owner.getUserId(),
            title,
            message,
            NotificationType.SYSTEM,
            String.valueOf(room.getRoomId())
    );

    emailService.sendRoomReactivationEmail(
            owner.getEmail(),
            owner.getFullName(),
            property.getPropertyName(),
            room.getRoomName()
    );
}

// ============================================================
// HELPER MAP ROOM DTO (chuẩn, không duplicate)
// ============================================================
private RoomResponseDTO mapToRoomDTO(Room room) {
    // Dùng constructor đã fix (area, active, weekendPrice, ...)
    RoomResponseDTO dto = new RoomResponseDTO(room);

    // Set currency and converted prices
    dto.setCurrency(currencyService.getCurrentCurrencySymbol());
    dto.setConvertedPricePerNight(currencyService.convertFromVND(room.getPricePerNight()));
    dto.setConvertedWeekendPrice(currencyService.convertFromVND(room.getWeekendPrice()));

    // Images (Signed URL)
    List<String> imageUrls = roomImageRepository.findByRoom_RoomId(room.getRoomId())
            .stream()
            .map(img -> fileStorageService.generateSignedUrl(img.getImageUrl()))
            .collect(Collectors.toList());
    dto.setImages(imageUrls);

    // Amenities
    List<String> amenityNames = roomAmenityRepository.findByRoom_RoomId(room.getRoomId())
            .stream()
            .filter(RoomAmenity::isActive)
            .map(ra -> ra.getAmenity().getAmenityName())
            .collect(Collectors.toList());
    dto.setAmenities(amenityNames);

    return dto;
}
}