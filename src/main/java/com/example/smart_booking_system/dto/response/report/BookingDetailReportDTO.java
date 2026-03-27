package com.example.smart_booking_system.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailReportDTO {
    private String bookingId;     // Mã đơn hàng (Rút gọn)
    private String customerName;  // Tên khách hàng
    private String createdDate;   // Ngày tạo (Bao gồm cả giờ)
    private String checkInDate;   // Ngày nhận phòng
    private Double totalPrice;    // Giá trị đơn
}