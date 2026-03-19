package com.example.smart_booking_system.scheduler;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Wallet;
import com.example.smart_booking_system.entity.WalletTransaction;
import com.example.smart_booking_system.enums.TransactionStatus;
import com.example.smart_booking_system.enums.TransactionType;
import com.example.smart_booking_system.repository.WalletRepository;
import com.example.smart_booking_system.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletScheduler {

    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletRepository walletRepository;

    /**
     * Chạy mỗi giờ một lần vào phút thứ 0 (VD: 1:00, 2:00...)
     * Lưu ý khi test: Bạn có thể đổi thành "0 * * * * *" để chạy mỗi phút một lần.
     * Gốc: 0 0 * * * * (chạy vào phút 0 của mỗi giờ) -> Test: 0 * * * * * (chạy vào mỗi phút)
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void clearPendingBalances() {
        log.info("⏳ [WALLET SCHEDULER] Đang quét các giao dịch PENDING để mở khóa tiền...");

        // 1. Lấy tất cả giao dịch đang bị giam (PENDING) và có loại là Doanh thu (BOOKING_REVENUE)
        List<WalletTransaction> pendingTransactions = walletTransactionRepository
                .findByStatusAndType(TransactionStatus.PENDING, TransactionType.BOOKING_REVENUE);

        LocalDateTime now = LocalDateTime.now();
        int clearedCount = 0;

        for (WalletTransaction transaction : pendingTransactions) {
            Booking booking = transaction.getBooking();

            // 2. Kiểm tra điều kiện: Booking đã checkout được quá 48 giờ
            // Dùng updatedAt vì nó chính là thời điểm Check-out được gọi
            if (booking != null && booking.getUpdatedAt() != null) {

                // Nếu thời gian update + 48h <= thời gian hiện tại
                if (booking.getUpdatedAt().plusHours(48).isBefore(now) || booking.getUpdatedAt().plusHours(48).isEqual(now)) {

                    Wallet wallet = transaction.getWallet();

                    // Chuyển tiền từ Pending sang Available
                    wallet.setPendingBalance(wallet.getPendingBalance().subtract(transaction.getAmount()));
                    wallet.setAvailableBalance(wallet.getAvailableBalance().add(transaction.getAmount()));

                    // Đổi trạng thái giao dịch thành CLEARED
                    transaction.setStatus(TransactionStatus.CLEARED);

                    // Lưu lại thay đổi
                    walletRepository.save(wallet);
                    walletTransactionRepository.save(transaction);

                    clearedCount++;
                    log.info("Đã mở khóa {} VND cho Wallet ID {} (Booking ID {})",
                            transaction.getAmount(), wallet.getId(), booking.getBookingId());
                }
            }
        }

        if (clearedCount > 0) {
            log.info("[WALLET SCHEDULER] Hoàn tất mở khóa cho {} giao dịch.", clearedCount);
        }
    }
}