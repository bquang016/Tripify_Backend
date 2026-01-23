package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.FileUploadResponse;
import com.example.smart_booking_system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestParam("files") List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("Không có file nào được upload!");
        }

        List<FileUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {

            // 1️⃣ Upload file vào R2 → trả về key
            String key = fileStorageService.storeImageFile(file);

            // 2️⃣ Tạo Signed URL (giống Cloudinary secure_url)
            String signedUrl = fileStorageService.generateSignedUrl(key);

            responses.add(new FileUploadResponse(
                    signedUrl,          // url hiển thị ảnh (giống Cloudinary secure_url)
                    key,                // key để lưu vào DB
                    file.getSize(),
                    file.getContentType()
            ));
        }

        // Trả về dạng Cloudinary: upload một file → trả object
        if (responses.size() == 1) {
            return ResponseEntity.ok(responses.get(0));
        }

        // Upload nhiều file → trả về list
        return ResponseEntity.ok(responses);
    }
}
