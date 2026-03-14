package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Payout;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final BookingRepository bookingRepo;
    private final PayoutRepository payoutRepo;
    private final PaymentService paymentService;

    // QUY ĐỊNH: Tripify thu 15% phí nền tảng. Dùng BigDecimal để tính toán không bị sai số
    private static final BigDecimal PLATFORM_COMMISSION_RATE = new BigDecimal("0.15");

    /**
     * Hàm tính toán và tạo bản kê doanh thu cho Owner trong 1 khoảng thời gian
     */
    @Transactional
    public Payout calculateAndCreatePayout(User owner, LocalDate startDate, LocalDate endDate) {

        // 1. Gọi Repo để lấy các Booking đã COMPLETED của Owner này trong kỳ
        List<Booking> completedBookings = bookingRepo.findCompletedBookingsForPayout(
                owner.getUserId(),
                startDate,
                endDate
        );

        if (completedBookings.isEmpty()) {
            return null; // Không có doanh thu kỳ này
        }

        // 2. Tính tổng tiền bằng BigDecimal
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Booking b : completedBookings) {
            if (b.getTotalPrice() != null) {
                totalRevenue = totalRevenue.add(b.getTotalPrice());
            }
        }

        // 3. Tính tiền hoa hồng và tiền thực nhận
        BigDecimal platformFee = totalRevenue.multiply(PLATFORM_COMMISSION_RATE);
        BigDecimal payoutAmount = totalRevenue.subtract(platformFee);

        // 4. Tạo bản ghi Payout (Chuyển BigDecimal về Double để lưu vào Entity Payout hiện tại)
        Payout payout = Payout.builder()
                .owner(owner)
                .totalRevenue(totalRevenue.doubleValue())
                .platformFee(platformFee.doubleValue())
                .payoutAmount(payoutAmount.doubleValue())
                .periodStart(startDate.atStartOfDay()) // Chuyển LocalDate thành LocalDateTime đầu ngày
                .periodEnd(endDate.atTime(23, 59, 59)) // Chuyển LocalDate thành LocalDateTime cuối ngày
                .status("PENDING") // Trạng thái chờ bắn tiền qua Stripe
                .build();

        return payoutRepo.save(payout);
    }
    @Transactional
    public Payout processPayout(Long payoutId) {
        Payout payout = payoutRepo.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản kê doanh thu"));

        if (!"PENDING".equals(payout.getStatus())) {
            throw new RuntimeException("Bản kê này đã được xử lý hoặc đang lỗi.");
        }

        User owner = payout.getOwner();

        // Kiểm tra xem Owner này có tài khoản Stripe không
        if (owner.getStripeAccountId() != null) {
            try {
                // Gọi API chuyển tiền của Stripe
                String transferId = paymentService.transferToOwner(
                        payout.getPayoutAmount().longValue(),
                        owner.getStripeAccountId()
                );

                // Thành công -> Cập nhật trạng thái
                payout.setStripeTransferId(transferId);
                payout.setStatus("COMPLETED");
                payout.setProcessedAt(LocalDateTime.now());

                // TODO: Gọi EmailService gửi mail báo "Ting ting" cho Owner

            } catch (Exception e) {
                payout.setStatus("FAILED");
                System.err.println("Lỗi chuyển tiền Stripe: " + e.getMessage());
            }
        } else {
            // Nếu Owner nhận qua ngân hàng thủ công (VNQR)
            payout.setStatus("MANUAL_REQUIRED"); // Đánh dấu để Kế toán tự chuyển tay
        }

        return payoutRepo.save(payout);
    }
}