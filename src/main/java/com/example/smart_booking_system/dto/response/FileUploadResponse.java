package com.example.smart_booking_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String fileDownloadUri;
    private String fileName;
    private long size;
    private String contentType;
}