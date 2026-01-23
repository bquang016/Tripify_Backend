package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.*;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyDetailsService {

    // ✅ Inject đúng các Repository cần thiết
    private final PropertyRepository propertyRepository;
    private final PropertyDetailRepository propertyDetailRepository;
    private final RoomRepository roomRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final PropertyImageRepository propertyImageRepository;

    @Transactional(readOnly = true)
    public PropertyDetailsResponseDTO getPropertyDetails(int propertyId) {

        // ============================
        // 1. Lấy Property (Thông tin chính)
        // ============================
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        // Lấy thông tin chi tiết (Diện tích)
        // Nếu chưa có detail thì trả về null hoặc object rỗng tùy logic, ở đây lấy area nếu có
        PropertyDetail propertyDetail = propertyDetailRepository.findByProperty_PropertyId(propertyId)
                .orElse(null);
        BigDecimal area = (propertyDetail != null) ? propertyDetail.getArea() : BigDecimal.ZERO;

        // Map Property → PropertyResponseDTO
        PropertyResponseDTO propertyDTO = new PropertyResponseDTO();
        propertyDTO.setPropertyId(property.getPropertyId());
        propertyDTO.setPropertyName(property.getPropertyName());
        propertyDTO.setPropertyType(property.getPropertyType());

        // Địa chỉ
        propertyDTO.setAddress(property.getAddress());
        propertyDTO.setCountry(property.getCountry());
        propertyDTO.setProvince(property.getProvince());
        propertyDTO.setCity(property.getCity());
        propertyDTO.setPostalCode(property.getPostalCode());
        propertyDTO.setArea(area);
        propertyDTO.setDescription(property.getDescription());
        propertyDTO.setLatitude(property.getLatitude());
        propertyDTO.setLongitude(property.getLongitude());
        propertyDTO.setPhoneContact(property.getPhoneContact());
        propertyDTO.setEmailContact(property.getEmailContact());
        propertyDTO.setRating(property.getRating());
        propertyDTO.setReviewCount(property.getReviewCount());
        propertyDTO.setActive(property.isActive());
        propertyDTO.setPropertyStatus(property.getPropertyStatus());
        propertyDTO.setCreatedAt(property.getCreatedAt());
        propertyDTO.setUpdatedAt(property.getUpdatedAt());


        // ============================
        // 2. Lấy Rooms theo propertyId
        // ============================
        // Cần đảm bảo RoomRepository có hàm findByPropertyId hoặc findByProperty_PropertyId
        List<Room> rooms = roomRepository.findByProperty_PropertyIdAndActiveTrue(propertyId);

        List<RoomResponseDTO> roomDTOs = rooms.stream().map(room -> {
            RoomResponseDTO dto = new RoomResponseDTO();
            dto.setRoomId(room.getRoomId());
            dto.setRoomName(room.getRoomName());
            dto.setRoomCategory(room.getRoomCategory());
            dto.setDescription(room.getDescription());
            dto.setCapacity(room.getCapacity());
            dto.setPricePerNight(room.getPricePerNight());
            dto.setRoomStatus(room.getRoomStatus());
            dto.setActive(room.isActive());
            // Lấy ID từ object Property trong Room
            dto.setPropertyId(room.getProperty().getPropertyId());
            return dto;
        }).collect(Collectors.toList());


        // ============================
        // 3. Lấy Amenities (Từ bảng PropertyAmenity)
        // ============================
        List<PropertyAmenity> amenities = propertyAmenityRepository.findByProperty_PropertyId(propertyId);

        List<PropertyAmenityResponseDTO> amenityDTOs = amenities.stream().map(a -> {
            PropertyAmenityResponseDTO dto = new PropertyAmenityResponseDTO();
            dto.setPropertyAmenityId(a.getPropertyAmenityId());
            dto.setPropertyId(a.getProperty().getPropertyId());

            // Lấy thông tin từ bảng Amenity gốc
            dto.setAmenityId(a.getAmenity().getAmenityId());
            dto.setAmenityName(a.getAmenity().getAmenityName());
            return dto;
        }).collect(Collectors.toList());


        // ============================
        // 4. Lấy Images (Từ bảng PropertyImage)
        // ============================
        List<PropertyImage> images = propertyImageRepository.findByProperty_PropertyId(propertyId);

        List<PropertyImageResponseDTO> imageDTOs = images.stream().map(img -> {
            // Sử dụng setter hoặc constructor tùy vào DTO của bạn
            PropertyImageResponseDTO dto = new PropertyImageResponseDTO();
            dto.setPropertyImageId(img.getPropertyImageId());
            dto.setPropertyId(img.getProperty().getPropertyId());
            dto.setImageUrl(img.getImageUrl());
            dto.setCover(img.isCover()); // ✅ Thêm mapping isCover
            return dto;
        }).collect(Collectors.toList());


        // ============================
        // 5. Gộp tất cả vào DTO tổng
        // ============================
        PropertyDetailsResponseDTO result = new PropertyDetailsResponseDTO();
        result.setProperty(propertyDTO);
        result.setRooms(roomDTOs);
        result.setAmenities(amenityDTOs);
        result.setImages(imageDTOs);

        return result;
    }
}