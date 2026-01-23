package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomImageResponseDTO;
import com.example.smart_booking_system.service.RoomImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.smart_booking_system.dto.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-images")
public class RoomImageController {

    private final RoomImageService roomImageService;

    public RoomImageController(RoomImageService roomImageService) {
        this.roomImageService = roomImageService;
    }

    @PostMapping("/upload-multiple/{roomId}")
    public ResponseEntity<?> uploadMultiple(
            @PathVariable int roomId,
            @RequestParam("file") List<MultipartFile> files) {

        List<RoomImageResponseDTO> dto =
                roomImageService.uploadMultipleRoomImages(roomId, files);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomImages(@PathVariable int roomId) {
        return ResponseEntity.ok(roomImageService.getImagesByRoomId(roomId));
    }

    @DeleteMapping("/delete/{roomId}/{imageId}")
    public ResponseEntity<?> deleteImage(
            @PathVariable int roomId,
            @PathVariable int imageId
    ) {
        try {
            roomImageService.deleteRoomImage(roomId, imageId);
            return ResponseEntity.ok(ApiResponse.success("Đã xóa ảnh thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @PutMapping("/{roomId}/{imageId}/set-cover")
    public ResponseEntity<?> setCoverImage(
            @PathVariable int roomId,
            @PathVariable int imageId
    ) {
        try {
            roomImageService.setCoverImage(roomId, imageId);
            return ResponseEntity.ok(ApiResponse.success("Đã đặt ảnh bìa thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
