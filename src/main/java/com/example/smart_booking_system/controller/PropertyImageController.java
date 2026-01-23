package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.PropertyImageResponseDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PropertyImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/property-images")
@RequiredArgsConstructor
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    // Upload nhiều ảnh cho property
    @PostMapping("/{propertyId}/upload")
    public ResponseEntity<?> uploadMultiple(
            @PathVariable int propertyId,
            @RequestParam("files") List<MultipartFile> files
    ) {

        List<PropertyImageResponseDTO> result =
                propertyImageService.uploadMultiplePropertyImages(propertyId, files);

        return ResponseEntity.ok(
                ApiResponse.success("Upload thành công", result)
        );
    }

    // Lấy danh sách ảnh theo property
    @GetMapping("/{propertyId}")
    public ResponseEntity<?> getByProperty(
            @PathVariable int propertyId
    ) {

        List<PropertyImageResponseDTO> images =
                propertyImageService.getImagesByPropertyId(propertyId);

        return ResponseEntity.ok(
                ApiResponse.success(images)
        );
    }

    // Xoá ảnh
    @DeleteMapping("/{propertyId}/{imageId}")
    public ResponseEntity<?> delete(
            @PathVariable int propertyId,
            @PathVariable int imageId
    ) {

        propertyImageService.deletePropertyImage(propertyId, imageId);

        return ResponseEntity.ok(
                ApiResponse.success("Đã xóa ảnh")
        );
    }

    // Đặt ảnh bìa
    @PutMapping("/{propertyId}/{imageId}/cover")
    public ResponseEntity<?> setCoverImage(
            @PathVariable int propertyId,
            @PathVariable int imageId
    ) {

        propertyImageService.setCoverImage(propertyId, imageId);

        return ResponseEntity.ok(
                ApiResponse.success("Đặt ảnh bìa thành công")
        );
    }
}
