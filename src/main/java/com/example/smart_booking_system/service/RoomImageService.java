package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomImageResponseDTO;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomImage;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.RoomImageRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomImageService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final FileStorageService fileStorageService;

    public RoomImageService(RoomRepository roomRepository,
                            RoomImageRepository roomImageRepository,
                            FileStorageService fileStorageService) {
        this.roomRepository = roomRepository;
        this.roomImageRepository = roomImageRepository;
        this.fileStorageService = fileStorageService;
    }

    // ================== SET COVER ================== //
    @Transactional
    public void setCoverImage(int roomId, int imageId) {

        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (image.getRoom().getRoomId() != roomId) {
            throw new BadRequestException("Image does not belong to this room");
        }

        roomImageRepository.resetCoverImageByRoomId(roomId);

        image.setCover(true);
        roomImageRepository.save(image);
    }

    // ================== UPLOAD MULTIPLE ================== //
    public List<RoomImageResponseDTO> uploadMultipleRoomImages(int roomId, List<MultipartFile> files) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("Room not found"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files uploaded");
        }

        List<RoomImageResponseDTO> result = new ArrayList<>();

        // ⭐ LƯU ĐÚNG STRUCTURE: properties/{propertyId}/rooms/{roomId}
        String folder = "properties/" + room.getProperty().getPropertyId()
                + "/rooms/" + roomId;

        for (MultipartFile file : files) {

            // ⭐ Lưu file vào đúng folder cấu trúc
            String key = fileStorageService.storeImageFile(file, folder);

            RoomImage ri = new RoomImage();
            ri.setRoom(room);
            ri.setImageUrl(key);
            ri.setActive(true);

            RoomImage saved = roomImageRepository.save(ri);

            result.add(new RoomImageResponseDTO(
                    saved.getRoomImageId(),
                    roomId,
                    fileStorageService.generateSignedUrl(saved.getImageUrl()), // signed URL
                    saved.isActive(),
                    saved.isCover()
            ));
        }

        return result;
    }

    // ================== GET LIST ================== //
    public List<RoomImageResponseDTO> getImagesByRoomId(int roomId) {

        List<RoomImage> list = roomImageRepository.findActiveImagesByRoomId(roomId);

        return list.stream()
                .map(img -> new RoomImageResponseDTO(
                        img.getRoomImageId(),
                        img.getRoom().getRoomId(),
                        fileStorageService.generateSignedUrl(img.getImageUrl()), // signed URL
                        img.isActive(),
                        img.isCover()
                ))
                .collect(Collectors.toList());
    }

    // ================== DELETE ================== //
    public void deleteRoomImage(int roomId, int imageId) {

        RoomImage img = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new BadRequestException("Image not found"));

        if (img.getRoom().getRoomId() != roomId) {
            throw new BadRequestException("This image does not belong to this room");
        }

        fileStorageService.deleteFile(img.getImageUrl()); // dùng key đầy đủ

        img.setActive(false);
        roomImageRepository.save(img);
    }
}
