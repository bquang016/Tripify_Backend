package com.example.smart_booking_system.dto.request.promotion;

import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.enums.MembershipRank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PromotionRequestDTO {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    private String description;

    @NotNull(message = "Loại giảm giá là bắt buộc")
    private DiscountType discountType;

    private PromotionStatus status;

    private MembershipRank minMembershipRank;

    @NotNull(message = "Giá trị giảm là bắt buộc")
    @Min(value = 0)
    private BigDecimal discountValue;

    @NotNull(message = "Ngày bắt đầu là bắt buộc, không được là ngày trong quá khứ")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc là bắt buộc, không được nhỏ hơn ngày hiện tại")
    private LocalDateTime endDate;

    @Min(value = 0)
    private BigDecimal minBookingAmount;

    @Min(value = 0)
    private BigDecimal maxDiscountAmount;

    @Min(value = 1)
    private Integer usageLimit;

    private Integer propertyId;
}