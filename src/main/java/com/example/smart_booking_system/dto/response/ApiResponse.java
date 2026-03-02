package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.util.I18nUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;

    // Success response with data
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(I18nUtil.getMessage("operation.success"));
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response with custom message (tries to translate key, else returns original)
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(I18nUtil.getMessage(message));
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response without data
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(I18nUtil.getMessage(message));
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Error response
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(I18nUtil.getMessage(message));
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Error response with path
    public static <T> ApiResponse<T> error(String message, String path) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(I18nUtil.getMessage(message));
        response.setPath(path);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}