package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.PropertyResponseDTO;
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.request.property.PropertyRegistrationRequest;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.dto.response.property.PropertyMapDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PropertyService {

    Property addProperty(Property property, String ownerId);

    PropertyDetailDTO updateProperty(int id, Property updatedProperty);

    List<PropertyDetailDTO> getFeaturedProperties();

    List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status);

    PropertyDetailDTO reviewProperty(Integer propertyId, PropertyReviewDTO reviewDTO, String adminUsername);

    PropertyDetailDTO getPropertyDetailById(Integer id, LocalDate checkIn, LocalDate checkOut);

    PropertyDetailDTO submitPropertyApplication(PropertyApplicationSubmitDTO dto,
                                                List<MultipartFile> images,
                                                String ownerId);

    List<PropertyDetailDTO> getOwnerProperties(String ownerId);

    List<PropertyDetailDTO> getOwnerActiveProperties(String ownerId);

    List<PropertyMapDTO> findNearbyProperties(Double lat, Double lng, Double radius);

    List<PropertyDetailDTO> searchProperties(String keyword,
                                             Integer guests,
                                             LocalDate checkIn,
                                             LocalDate checkOut);

    boolean checkNameAvailability(String propertyName);

    boolean togglePropertyStatus(Integer propertyId, String ownerId);

    // THÊM MỚI: Method lấy danh sách Property đang hoạt động cho Admin (có phân trang)
    Page<PropertyResponseDTO> getAllActiveProperties(Pageable pageable);

    // THÊM MỚI: Method dừng hoạt động Property
    void suspendProperty(Integer propertyId, String reason);

    // [NEW] Mở lại hoạt động khách sạn (Re-activate)
    void activateProperty(Integer propertyId);

    // [NEW] Lấy danh sách Property theo trạng thái có phân trang (Dùng cho Admin Filter)
    // Hàm này hỗ trợ API /list mà chúng ta vừa tạo ở Controller
    Page<PropertyResponseDTO> getPropertiesByStatusPaginated(PropertyStatus status, Pageable pageable);

    Page<PropertyDetailDTO> searchPropertiesPaginated(
            String keyword,
            List<String> cities,    // ✅ Mới
            List<Integer> ratings,  // ✅ Mới
            Integer guests,
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean isManager,
            Pageable pageable
    );
    PropertyDetailDTO registerProperty(com.example.smart_booking_system.dto.request.property.PropertyRegistrationRequest request, String ownerId);

}
