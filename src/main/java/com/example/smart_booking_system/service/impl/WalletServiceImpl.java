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
import com.example.smart_booking_system.dto.request.owner.PayoutSettingsDTO;
import com.example.smart_booking_system.enums.PayoutMethod;
import com.example.smart_booking_system.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final com.example.smart_booking_system.repository.UserRepository userRepository;
    private final com.example.smart_booking_system.service.PaymentService paymentService;

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
    @Override
    @Transactional(readOnly = true)
    public PayoutSettingsDTO getPayoutSettings(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        Wallet wallet = getOrCreateWallet(owner);

        return PayoutSettingsDTO.builder()
                .defaultPayoutMethod(wallet.getDefaultPayoutMethod().name())
                .bankName(wallet.getBankName())
                .accountHolderName(wallet.getAccountHolderName())
                .accountNumber(wallet.getAccountNumber())
                .stripeAccountId(wallet.getStripeAccountId())
                .cardLast4(wallet.getCardLast4())
                .cardBrand(wallet.getCardBrand())
                .build();
    }

    @Override
    @Transactional
    public PayoutSettingsDTO updatePayoutSettings(String ownerEmail, PayoutSettingsDTO request) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        // Dùng hàm getOrCreateWallet đã tạo ở các bước trước
        Wallet wallet = getOrCreateWallet(owner);

        // 1. Cập nhật phương thức thanh toán mặc định
        if (request.getDefaultPayoutMethod() != null) {
            try {
                PayoutMethod requestedMethod = PayoutMethod.valueOf(request.getDefaultPayoutMethod());
                wallet.setDefaultPayoutMethod(requestedMethod);
            } catch (IllegalArgumentException e) {
                wallet.setDefaultPayoutMethod(PayoutMethod.NONE);
            }
        }

        // 2. Cập nhật thông tin Ngân hàng (Bank)
        if (request.getBankName() != null) wallet.setBankName(request.getBankName());
        if (request.getAccountHolderName() != null) wallet.setAccountHolderName(request.getAccountHolderName());
        if (request.getAccountNumber() != null) wallet.setAccountNumber(request.getAccountNumber());

        // 3. Cập nhật thông tin Stripe qua Token
        if (request.getStripeToken() != null && !request.getStripeToken().isEmpty()) {
            try {
                String stripeAccountId = wallet.getStripeAccountId();
                com.stripe.model.Card stripeCard = null;

                if (stripeAccountId == null || stripeAccountId.isEmpty()) {
                    // KỊCH BẢN A: Chủ nhà chưa có tài khoản Stripe Connect -> Tạo mới toàn bộ
                    stripeAccountId = paymentService.createStripeConnectedAccount(owner.getEmail(), request.getStripeToken());
                    wallet.setStripeAccountId(stripeAccountId);

                    // Đồng bộ lưu ID vào bảng User (vì DataInitializer/Entity của bạn có trường này ở User)
                    owner.setStripeAccountId(stripeAccountId);
                    userRepository.save(owner);

                    // Lấy data thẻ vừa khởi tạo
                    stripeCard = paymentService.getConnectAccountDefaultCard(stripeAccountId);
                } else {
                    // KỊCH BẢN B: Chủ nhà đã có Connect Account -> Gọi Stripe gắn thẻ mới thay thế
                    com.stripe.model.ExternalAccount externalAccount = paymentService.addExternalAccountToConnect(stripeAccountId, request.getStripeToken());
                    if (externalAccount instanceof com.stripe.model.Card) {
                        stripeCard = (com.stripe.model.Card) externalAccount;
                    }
                }

                // Lưu lại thông tin hiển thị (4 số cuối, hãng thẻ) xuống DB
                if (stripeCard != null) {
                    wallet.setCardLast4(stripeCard.getLast4());
                    wallet.setCardBrand(stripeCard.getBrand());
                }

            } catch (Exception e) {
                // Quăng lỗi ra để Controller bắt và trả về Frontend (Ví dụ: Thẻ hết hạn, CVV sai...)
                throw new RuntimeException("Lỗi xác thực thẻ từ Stripe: " + e.getMessage());
            }
        }

        walletRepository.save(wallet);

        // Trả về dữ liệu mới nhất
        return getPayoutSettings(ownerEmail);
    }
    @Override
    @Transactional
    public PayoutSettingsDTO deletePaymentMethod(String ownerEmail, String type) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        Wallet wallet = getOrCreateWallet(owner);

        if ("bank".equalsIgnoreCase(type)) {
            wallet.setBankName(null);
            wallet.setAccountHolderName(null);
            wallet.setAccountNumber(null);
            if (wallet.getDefaultPayoutMethod() == PayoutMethod.BANK_TRANSFER) {
                wallet.setDefaultPayoutMethod(PayoutMethod.NONE);
            }
        } else if ("stripe".equalsIgnoreCase(type)) {
            wallet.setStripeAccountId(null);
            wallet.setCardLast4(null);
            wallet.setCardBrand(null);
            if (wallet.getDefaultPayoutMethod() == PayoutMethod.STRIPE) {
                wallet.setDefaultPayoutMethod(PayoutMethod.NONE);
            }
        }

        walletRepository.save(wallet);
        return getPayoutSettings(ownerEmail);
    }
    // Hàm hỗ trợ: Tìm Ví, nếu chưa có thì tự động tạo mới
    private Wallet getOrCreateWallet(User owner) {
        return walletRepository.findByOwner(owner)
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .owner(owner)
                            .availableBalance(BigDecimal.ZERO)
                            .pendingBalance(BigDecimal.ZERO)
                            .defaultPayoutMethod(PayoutMethod.NONE)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }
}