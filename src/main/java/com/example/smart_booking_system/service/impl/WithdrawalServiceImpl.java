package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.Wallet;
import com.example.smart_booking_system.entity.WalletTransaction;
import com.example.smart_booking_system.entity.WithdrawalRequest;
import com.example.smart_booking_system.enums.TransactionStatus;
import com.example.smart_booking_system.enums.TransactionType;
import com.example.smart_booking_system.enums.WithdrawalStatus;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.repository.WalletRepository;
import com.example.smart_booking_system.repository.WalletTransactionRepository;
import com.example.smart_booking_system.repository.WithdrawalRequestRepository;
import com.example.smart_booking_system.service.PaymentService;
import com.example.smart_booking_system.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService; // Inject PaymentService để gọi Stripe

    // Cấu hình hạn mức rút tối thiểu (Ví dụ: 500,000 VNĐ)
    private static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("500000");

    @Override
    @Transactional
    public WithdrawalRequest requestWithdrawal(String ownerUserId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByOwner_UserId(ownerUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của Chủ nhà."));

        // 1. Kiểm tra điều kiện
        if (amount.compareTo(MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new RuntimeException("Số tiền rút phải lớn hơn hoặc bằng " + MIN_WITHDRAWAL_AMOUNT);
        }

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư khả dụng không đủ để thực hiện giao dịch.");
        }

        // 2. Trừ tiền ở số dư khả dụng ngay lập tức để tránh rút Double
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        walletRepository.save(wallet);

        // 3. Tạo yêu cầu rút tiền
        WithdrawalRequest request = WithdrawalRequest.builder()
                .wallet(wallet)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .build();
        request = withdrawalRequestRepository.save(request);

        // 4. Tạo lịch sử giao dịch Ví (Trạng thái đang xử lý)
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .withdrawalRequestId(request.getId())
                .build();
        walletTransactionRepository.save(transaction);

        log.info("📩 Chủ nhà {} đã yêu cầu rút {} VNĐ. Request ID: {}", ownerUserId, amount, request.getId());
        return request;
    }

    @Override
    @Transactional
    public WithdrawalRequest approveWithdrawal(Long requestId, String adminUserId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền."));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý.");
        }

        User owner = request.getWallet().getOwner();

        try {
            // 1. Gọi Stripe để chuyển tiền
            String description = "Tripify: Rút tiền, Mã lệnh: " + request.getId();
            String stripeTransferId = paymentService.processStripeTransfer(
                    request.getAmount().longValue(),
                    owner.getStripeAccountId(),
                    description
            );

            // 2. Nếu thành công, cập nhật trạng thái Request
            request.setStatus(WithdrawalStatus.COMPLETED);
            request.setStripeTransferId(stripeTransferId);
            withdrawalRequestRepository.save(request);

            // 3. Cập nhật trạng thái giao dịch Ví thành CLEARED (hoàn tất)
            updateWalletTransactionStatus(request.getId(), TransactionStatus.CLEARED);

            log.info("✅ Admin {} đã duyệt lệnh rút tiền {}. Stripe Transfer ID: {}", adminUserId, requestId, stripeTransferId);
            return request;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Stripe chuyển tiền cho Request {}: {}", requestId, e.getMessage());

            // Xử lý lỗi: Đổi trạng thái thành FAILED và hoàn tiền lại vào Ví
            request.setStatus(WithdrawalStatus.FAILED);
            request.setAdminNote("Lỗi cổng thanh toán: " + e.getMessage());
            withdrawalRequestRepository.save(request);

            refundToWallet(request);
            updateWalletTransactionStatus(request.getId(), TransactionStatus.FAILED);

            throw new RuntimeException("Lỗi chuyển tiền qua Stripe: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public WithdrawalRequest rejectWithdrawal(Long requestId, String adminNote, String adminUserId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền."));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý.");
        }

        // 1. Cập nhật trạng thái từ chối
        request.setStatus(WithdrawalStatus.REJECTED);
        request.setAdminNote(adminNote);
        withdrawalRequestRepository.save(request);

        // 2. Hoàn lại tiền vào Ví khả dụng
        refundToWallet(request);

        // 3. Cập nhật giao dịch Ví thành FAILED
        updateWalletTransactionStatus(request.getId(), TransactionStatus.FAILED);

        log.info("🚫 Admin {} đã từ chối lệnh rút tiền {}. Lý do: {}", adminUserId, requestId, adminNote);
        return request;
    }

    @Override
    public List<WithdrawalRequest> getWithdrawalHistoryByOwner(String ownerUserId) {
        Wallet wallet = walletRepository.findByOwner_UserId(ownerUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví."));
        return withdrawalRequestRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    @Override
    public List<WithdrawalRequest> getAllWithdrawalRequests() {
        return withdrawalRequestRepository.findAll();
    }

    // --- Private Helper Methods ---

    private void refundToWallet(WithdrawalRequest request) {
        Wallet wallet = request.getWallet();
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
        walletRepository.save(wallet);
        log.info("⏪ Đã hoàn {} VNĐ lại vào số dư khả dụng cho Ví {}", request.getAmount(), wallet.getId());
    }

    private void updateWalletTransactionStatus(Long withdrawalRequestId, TransactionStatus status) {
        walletTransactionRepository.findAll().stream()
                .filter(t -> withdrawalRequestId.equals(t.getWithdrawalRequestId()))
                .forEach(t -> {
                    t.setStatus(status);
                    walletTransactionRepository.save(t);
                });
    }
}