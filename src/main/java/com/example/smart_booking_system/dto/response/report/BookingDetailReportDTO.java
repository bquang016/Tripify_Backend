package com.example.smart_booking_system.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailReportDTO {
    private String period;        // Thời gian
    private String customerName;  // Tên khách hàng
    private String email;         // Email
    private String createdDate;   // Ngày tạo đơn
    private String checkInDate;   // Check-in
    private String status;        // Trạng thái
    private String propertyName;  // Cơ sở lưu trú (Cột mới thêm)
    private Double totalPrice;    // Số tiền
}