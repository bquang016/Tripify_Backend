package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.Wallet;
import com.example.smart_booking_system.entity.WalletTransaction;
import com.example.smart_booking_system.enums.TransactionStatus;
import com.example.smart_booking_system.enums.TransactionType;
import com.example.smart_booking_system.repository.WalletRepository;
import com.example.smart_booking_system.repository.WalletTransactionRepository;
import com.example.smart_booking_system.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    // Fix cứng hoa hồng nền tảng là 15%
    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.15");

    @Override
    @Transactional
    public void createWallet(User owner) {
        if (walletRepository.findByOwner_UserId(owner.getUserId()).isEmpty()) {
            Wallet wallet = Wallet.builder()
                    .owner(owner)
                    .availableBalance(BigDecimal.ZERO)
                    .pendingBalance(BigDecimal.ZERO)
                    .build();
            walletRepository.save(wallet);
            log.info("Đã tạo ví tự động cho Owner ID: {}", owner.getUserId());
        }
    }

    @Override
    @Transactional
    public void addBookingRevenueToPending(Booking booking) {
        User owner = booking.getProperty().getOwner();

        // 1. Lấy ví của Owner. Nếu đây là Owner cũ chưa có ví -> Tự động tạo luôn cho chắc chắn (Fallback logic)
        Wallet wallet = walletRepository.findByOwner_UserId(owner.getUserId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .owner(owner)
                            .availableBalance(BigDecimal.ZERO)
                            .pendingBalance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });

        // 2. Tính toán doanh thu
        BigDecimal totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal refundAmount = booking.getRefundAmount() != null ? booking.getRefundAmount() : BigDecimal.ZERO;

        // Doanh thu tính phí = Tổng tiền - Tiền đã hoàn (nếu có)
        BigDecimal netRevenue = totalPrice.subtract(refundAmount);

        // Nếu doanh thu <= 0 thì không xử lý
        if (netRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Booking ID {} có netRevenue <= 0, bỏ qua cộng tiền ví.", booking.getBookingId());
            return;
        }

        // Tính phí sàn 15% và tiền thực nhận 85%
        BigDecimal platformFee = netRevenue.multiply(PLATFORM_FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ownerRevenue = netRevenue.subtract(platformFee);

        // 3. Cộng tiền vào pendingBalance của Ví
        wallet.setPendingBalance(wallet.getPendingBalance().add(ownerRevenue));
        walletRepository.save(wallet);

        // 4. Tạo lịch sử giao dịch (Wallet Transaction)
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(ownerRevenue)
                .type(TransactionType.BOOKING_REVENUE)
                .status(TransactionStatus.PENDING) // Tiền đang bị giam
                .booking(booking)
                .build();
        walletTransactionRepository.save(transaction);

        log.info("Đã cộng {} (sau khi trừ 15% phí) vào ví Pending của Owner {} cho Booking ID {}",
                ownerRevenue, owner.getUserId(), booking.getBookingId());
    }
    @Override
    public Wallet getWalletByOwnerId(String ownerId) {
        return walletRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của Chủ nhà này"));
    }
}