package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.RatingRequestDTO;
import com.example.smart_booking_system.dto.response.RatingResponseDTO;
import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.entity.RatingImage;
import com.example.smart_booking_system.service.RatingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // --- HÀM MAPPER (Chuyển Entity -> DTO) ---
    private RatingResponseDTO mapToDTO(Rating rating) {
        RatingResponseDTO dto = new RatingResponseDTO();
        dto.setRatingId(rating.getRatingId());
        dto.setStars(rating.getRating());
        dto.setComment(rating.getComment());
        dto.setHidden(rating.isHidden());
        dto.setCreatedAt(rating.getCreatedAt());
        dto.setUserId(rating.getUserId().getUserId());
        dto.setUserAvatar(rating.getUserId().getUserDetail().getProfilePhotoUrl());
        dto.setIsPinned(Boolean.TRUE.equals(rating.getIsPinned()));

        // Lấy thông tin từ các quan hệ (đã được load hoặc proxy an toàn khi gọi getter ID)
        if (rating.getBookingId() != null) {
            dto.setBookingId(rating.getBookingId().getBookingId());
            // Map thêm tên khách sạn/phòng nếu booking đã fetch property/room
            if (rating.getBookingId().getProperty() != null) {
                dto.setPropertyName(rating.getBookingId().getProperty().getPropertyName());
            }
            if (rating.getBookingId().getRoom() != null) {
                dto.setRoomName(rating.getBookingId().getRoom().getRoomName());
            }
        }

        if (rating.getUserId() != null) {
            dto.setUserName(rating.getUserId().getFullName()); // Giả sử User có getFullName
        }

        // Map danh sách ảnh
        if (rating.getImages() != null) {
            List<String> urls = rating.getImages().stream()
                    .map(RatingImage::getImageUrl)
                    .collect(Collectors.toList());
            dto.setImages(urls);
        }

        return dto;
    }

    // --- CREATE ---
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RatingResponseDTO> createRating(
            @RequestPart("data") String ratingJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        RatingRequestDTO dto = mapper.readValue(ratingJson, RatingRequestDTO.class);

        Rating savedRating = ratingService.createRating(dto, files);
        return ResponseEntity.ok(mapToDTO(savedRating));
    }

    // --- UPDATE ---
    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RatingResponseDTO> updateRating(
            @PathVariable int id,
            @RequestPart("data") String ratingJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        RatingRequestDTO dto = mapper.readValue(ratingJson, RatingRequestDTO.class);

        Rating updatedRating = ratingService.updateRating(id, dto, files);
        return ResponseEntity.ok(mapToDTO(updatedRating));
    }

    // --- GET BY BOOKING ---
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Page<RatingResponseDTO>> getByBooking(
            @PathVariable int bookingId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Rating> ratings = ratingService.getRatingByBooking(bookingId, page);
        // Convert Page<Entity> -> Page<DTO>
        Page<RatingResponseDTO> dtoPage = ratings.map(this::mapToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    // --- GET BY PROPERTY ---
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<Page<RatingResponseDTO>> getByProperty(
            @PathVariable int propertyId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Rating> ratings = ratingService.getRatingsForProperty(propertyId, page);
        return ResponseEntity.ok(ratings.map(this::mapToDTO));
    }

    // --- CÁC API KHÁC (Cũng nên sửa return type tương tự) ---

    @GetMapping("/{id}")
    public ResponseEntity<RatingResponseDTO> getRatingById(@PathVariable int id) {
        return ResponseEntity.ok(mapToDTO(ratingService.getRatingById(id)));
    }

    // Giữ nguyên logic delete/hide nhưng có thể đổi return type nếu cần
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'OWNER', 'SUPER_ADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteRating(@PathVariable int id) {
        ratingService.deleteRating(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/hide/{id}")
    public ResponseEntity<RatingResponseDTO> hideRating(
            @PathVariable int id,
            @RequestParam boolean hide
    ) {
        return ResponseEntity.ok(mapToDTO(ratingService.hideRating(id, hide)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SUPER_ADMIN')")
    @PatchMapping("/pin/{id}")
    public ResponseEntity<RatingResponseDTO> pinRating(
            @PathVariable int id,
            @RequestParam boolean pin
    ) {
        // Lưu ý: Để bảo mật chặt chẽ, trong Service nên check thêm:
        // Nếu là ROLE_OWNER thì rating này phải thuộc về khách sạn của họ.
        // Ở đây tạm thời giả định @PreAuthorize đã chặn user thường.

        return ResponseEntity.ok(mapToDTO(ratingService.pinRating(id, pin)));
    }


}