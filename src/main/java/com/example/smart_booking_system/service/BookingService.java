package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.time.DayOfWeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final PropertyPoliciesRepository policiesRepo;
    private final EmailService emailService;
    private final PaymentRepository paymentRepo;
    private final PromotionRepository promotionRepo;
    private final RatingRepository ratingRepository;
    private final RefundRequestRepository refundRepo;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // ================================
    // CREATE BOOKING (chặt chẽ, capacity cho mọi loại)
    // ================================
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO req) {

        // --- 1) Kiểm tra tồn tại cơ bản (Giữ nguyên) ---
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));

        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found: " + req.getPropertyId()));

        // Logic chọn Room (Giữ nguyên)
        boolean requiresWholeRoom =
                property.getPropertyType() == PropertyType.VILLA ||
                        property.getPropertyType() == PropertyType.HOMESTAY;

        Room room;
        if (requiresWholeRoom) {
            if (req.getRoomId() != null) {
                throw new RuntimeException("Do not send roomId for Villa/Homestay. Booking is always the WHOLE room.");
            }
            room = roomRepo.findByPropertyIdAndCategory(req.getPropertyId(), RoomCategory.WHOLE)
                    .orElseThrow(() -> new RuntimeException("Whole room not found for property " + req.getPropertyId()));
        } else {
            if (req.getRoomId() == null) {
                throw new RuntimeException("roomId is required for HOTEL/RESORT booking");
            }
            room = roomRepo.findById(req.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));

            if (room.getProperty() == null || room.getProperty().getPropertyId() != req.getPropertyId()) {
                throw new RuntimeException("Room does not belong to the given property");
            }
        }

        // --- 3) Validate Guest (Giữ nguyên) ---
        if (req.getGuestCount() == null || req.getGuestCount() <= 0) {
            throw new RuntimeException("guestCount must be greater than 0");
        }
        if (req.getGuestCount() > room.getCapacity()) {
            throw new RuntimeException("guestCount exceeds room capacity");
        }

        // --- 4) Validate Date (Giữ nguyên) ---
        if (req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new RuntimeException("Dates are required");
        }
        if (!req.getCheckInDate().isBefore(req.getCheckOutDate())) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }

        // --- 5) Check Overlapping (Giữ nguyên) ---
        List<Booking> overlapping = bookingRepo.findConfirmedOverlappingByRoomId(
                room.getRoomId(), req.getCheckInDate(), req.getCheckOutDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Room is already booked in the selected dates");
        }

        // --- 6) Tính tiền (ĐÃ SỬA LỖI: Tính giá theo từng đêm, có phân biệt cuối tuần) ---
        BigDecimal total = BigDecimal.ZERO;
        LocalDate currentDate = req.getCheckInDate();

        // Giá cơ bản phòng (Giả định là giá ngày thường: weekdayPrice)
        BigDecimal weekdayPrice = room.getPricePerNight();


        BigDecimal weekendPrice = room.getWeekendPrice() != null
                ? room.getWeekendPrice()
                : weekdayPrice;

        // Đảm bảo giá cuối tuần không thấp hơn giá ngày thường (trường hợp Factor < 1)
        if (weekendPrice.compareTo(weekdayPrice) < 0) {
            weekendPrice = weekdayPrice;
        }

        // Vòng lặp tính tổng giá trị theo từng đêm
        while (currentDate.isBefore(req.getCheckOutDate())) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            // Xác định xem ngày đó có phải là Thứ Bảy hoặc Chủ Nhật không
            boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            if (isWeekend) {
                // Áp dụng giá cuối tuần
                total = total.add(weekendPrice);
            } else {
                // Áp dụng giá ngày thường
                total = total.add(weekdayPrice);
            }

            // Chuyển sang ngày tiếp theo
            currentDate = currentDate.plusDays(1);
        }

        // --- 7) TẠO BOOKING & LƯU THÔNG TIN KHÁCH ---
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProperty(property);
        booking.setRoom(room);
        booking.setCheckInDate(req.getCheckInDate());
        booking.setCheckOutDate(req.getCheckOutDate());
        booking.setGuestCount(req.getGuestCount());
        booking.setTotalPrice(total);
        booking.setPenaltyAmount(BigDecimal.ZERO);
        booking.setRefundAmount(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        // Xử lý thông tin người ở
        if (req.isBookingForSelf()) {
            booking.setCustomerName(user.getFullName());
            booking.setCustomerPhone(user.getPhoneNumber());
            booking.setCustomerEmail(user.getEmail());
        } else {
            if (req.getContactName() == null || req.getContactPhone() == null) {
                throw new RuntimeException("Contact info is required when booking for others");
            }
            booking.setCustomerName(req.getContactName());
            booking.setCustomerPhone(req.getContactPhone());
            booking.setCustomerEmail(req.getContactEmail());
        }
        booking.setSpecialRequest(req.getSpecialRequest());

        bookingRepo.save(booking);

        // --- 8) Tạo Payment PENDING (Giữ nguyên) ---
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTotalAmount(total);
        payment.setPaymentMethod(null);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // --- 9) Logic Gửi Email (Giữ nguyên) ---
        try {
            emailService.sendPaymentReminderEmail(
                    booking.getCustomerEmail(),
                    booking.getCustomerName(),
                    String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice().toString()
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }

        // ========================================================================
        // 🔥 [CẬP NHẬT] GỬI THÔNG BÁO (NOTIFICATION)
        // ========================================================================
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String propertyName = property.getPropertyName();

            // 1. Gửi cho OWNER (Người nhận được đơn)
            User owner = property.getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(), // [SỬA] Truyền String ID
                        "Đơn đặt phòng mới #" + booking.getBookingId(),
                        "Khách hàng " + booking.getCustomerName() + " vừa đặt phòng tại " + propertyName + ". Trạng thái: Chờ thanh toán.",
                        NotificationType.BOOKING_RECEIVED, // [SỬA] Dùng Type của Owner
                        relatedId
                );
            }

            // 2. Gửi cho CUSTOMER (Người đặt) - Để họ biết đã tạo đơn thành công và cần thanh toán
            notificationService.sendNotification(
                    user.getUserId(), // [SỬA] Truyền String ID
                    "Đặt phòng thành công - Chờ thanh toán",
                    "Bạn vừa đặt phòng tại " + propertyName + ". Vui lòng hoàn tất thanh toán để giữ phòng.",
                    NotificationType.BOOKING_SUCCESS, // [SỬA] Dùng Type của Customer (Tạm coi là success bước giữ chỗ)
                    relatedId
            );

        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo (không ảnh hưởng booking): " + e.getMessage());
        }

        return convertToDTO(booking);
    }

    // ================================
    // CANCEL BOOKING
    // ================================
    @Transactional
    public BookingResponseDTO cancelBooking(int bookingId) {
        // 1. Kiểm tra tồn tại
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        String relatedId = String.valueOf(booking.getBookingId());

        // --- TRƯỜNG HỢP 1: CHƯA THANH TOÁN (Hủy ngay) ---
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPenaltyAmount(BigDecimal.ZERO);
            booking.setRefundAmount(BigDecimal.ZERO);

            Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.REJECTED);
                paymentRepo.save(payment);
            }

            bookingRepo.save(booking);

            // Thông báo cho KHÁCH
            notificationService.sendNotification(
                    booking.getUser().getUserId(),
                    "Hủy đặt phòng thành công",
                    "Đơn đặt phòng #" + booking.getBookingId() + " đã được hủy thành công. Bạn không bị tính phí.",
                    NotificationType.BOOKING_CANCELLED,
                    relatedId
            );

            return convertToDTO(booking);
        }

        // --- TRƯỜNG HỢP 2: ĐÃ THANH TOÁN (Tính toán phí phạt & hoàn tiền) ---
        PropertyPolicies policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = booking.getCheckInDate();

        BigDecimal refundAmount;
        BigDecimal penaltyAmount;

        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, checkInDate);

        // Logic tính phí phạt
        if (daysUntilCheckIn <= 1) {
            penaltyAmount = booking.getTotalPrice();
            refundAmount = BigDecimal.ZERO;
        } else {
            boolean isFreeCancellation = false;
            if (policies != null && Boolean.TRUE.equals(policies.isAllowFreeCancellation())) {
                Integer freeDays = policies.getFreeCancellationDays();
                if (freeDays == null) freeDays = 0;
                LocalDate freeDeadline = checkInDate.minusDays(freeDays);

                if (!today.isAfter(freeDeadline)) {
                    isFreeCancellation = true;
                }
            }

            if (isFreeCancellation) {
                penaltyAmount = BigDecimal.ZERO;
                refundAmount = booking.getTotalPrice();
            } else {
                penaltyAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.30)); // Phí 30%
                refundAmount = booking.getTotalPrice().subtract(penaltyAmount);
            }
        }

        // Cập nhật Booking
        booking.setPenaltyAmount(penaltyAmount);
        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        // (ĐÃ BỎ ĐOẠN TỰ ĐỘNG TẠO REFUND REQUEST Ở ĐÂY THEO YÊU CẦU CỦA BẠN)

        // 3. Gửi Email thông báo
        try {
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
            emailService.sendCancellationRequestReceivedEmail(
                    emailTo, nameTo, String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice(), penaltyAmount, refundAmount
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail cancel: " + e.getMessage());
        }

        // ========================================================================
        // 🔥 CẬP NHẬT GỬI THÔNG BÁO (NOTIFICATION)
        // ========================================================================
        try {
            // 1. Gửi cho CUSTOMER
            String customerMsg;
            // Logic mới: Dựa vào số tiền hoàn tính được để thông báo
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                customerMsg = "Đơn #" + booking.getBookingId() + " đã hủy. Số tiền hoàn dự kiến: "
                        + String.format("%,.0f", refundAmount) + " VNĐ. Vui lòng liên hệ hỗ trợ nếu cần thêm thông tin.";
            } else {
                customerMsg = "Đơn #" + booking.getBookingId() + " đã hủy. Rất tiếc, bạn không được hoàn tiền do quá hạn hủy miễn phí.";
            }

            notificationService.sendNotification(
                    booking.getUser().getUserId(),
                    "Đã hủy đặt phòng",
                    customerMsg,
                    NotificationType.BOOKING_CANCELLED,
                    relatedId
            );

            // 2. Gửi cho OWNER
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(),
                        "Khách đã hủy phòng #" + booking.getBookingId(),
                        "Khách hàng đã hủy đơn đặt phòng. Lịch phòng đã được mở lại.",
                        NotificationType.BOOKING_CANCELLED_BY_GUEST,
                        relatedId
                );
            }

            // [BỔ SUNG] 3. Gửi cho ADMIN (Nếu có tiền cần hoàn trả)
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                notificationService.sendToAllAdmins(
                        "Yêu cầu hoàn tiền mới",
                        "Đơn phòng #" + booking.getBookingId() + " đã hủy. Số tiền cần hoàn: " + String.format("%,.0f", refundAmount) + " VNĐ.",
                        NotificationType.ADMIN_NEW_REFUND_REQUEST,
                        relatedId // Truyền ID booking để Admin click vào xem chi tiết
                );
            }

        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo: " + e.getMessage());
        }

        return convertToDTO(booking);
    }



    // ================================
    // HÀM MỚI: TỰ ĐỘNG QUÉT ĐƠN QUÁ HẠN (Scheduler sẽ gọi hàm này)
    // ================================
    @Transactional
    public void scanAndCancelExpiredBookings() {
        // Thời gian hiện tại trừ đi 5 phút
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);

        // Tìm các đơn PENDING_PAYMENT được tạo trước thời điểm expirationTime
        List<Booking> expiredBookings = bookingRepo.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT,
                expirationTime
        );

        if (!expiredBookings.isEmpty()) {
            logger.info("Tìm thấy {} đơn hàng quá hạn thanh toán (5 phút). Đang hủy...", expiredBookings.size());

            for (Booking booking : expiredBookings) {
                try {
                    // Gọi lại hàm cancelBooking ở trên để tái sử dụng logic
                    cancelBooking(booking.getBookingId());
                    logger.info("Đã tự động hủy đơn booking ID: {}", booking.getBookingId());
                } catch (Exception e) {
                    logger.error("Lỗi khi tự động hủy đơn ID {}: {}", booking.getBookingId(), e.getMessage());
                }
            }
        }
    }

    // ================================
    // APPROVE REFUND (Admin thực hiện)
    // ================================
    @Transactional
    public void approveRefund(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        RefundRequest refundRequest = refundRepo.findByBooking(booking).orElseThrow(() -> new RuntimeException("Refund Request not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new RuntimeException("Payment not found"));

        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setResolveDate(LocalDateTime.now());
        refundRequest.setAdminNote("Admin approved.");
        refundRepo.save(refundRequest);

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmount(refundRequest.getAmount());
        paymentRepo.save(payment);

        try {
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

            emailService.sendCancellationSuccessEmail(
                    emailTo, nameTo, String.valueOf(booking.getBookingId()),
                    String.format("%,.0f", booking.getRefundAmount()),
                    String.format("%,.0f", booking.getPenaltyAmount())
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail success refund: " + e.getMessage());
        }
        try {
            notificationService.sendNotification(
                    booking.getUser().getUserId(),
                    "Hoàn tiền thành công",
                    "Yêu cầu hoàn tiền cho đơn #" + booking.getBookingId() + " đã được chấp thuận. Tiền sẽ về tài khoản sau 3-5 ngày làm việc.",
                    NotificationType.REFUND_PROCESSED,
                    String.valueOf(booking.getBookingId())
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo approve refund: " + e.getMessage());
        }
    }

    // 3. CHECK-IN (Vận hành)
    @Transactional
    public void checkInBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ được Check-in đơn ĐÃ XÁC NHẬN");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepo.save(booking);
    }

    // 4. CHECK-OUT (Hoàn tất)
    @Transactional
    public void checkOutBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Cho phép checkout nếu đang ở hoặc đã confirm (quên checkin)
        if (booking.getStatus() != BookingStatus.CHECKED_IN && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Trạng thái không hợp lệ để Check-out");
        }

        // 1. Cập nhật trạng thái Booking
        booking.setStatus(BookingStatus.COMPLETED);

        // 2. TÍCH ĐIỂM & THĂNG HẠNG
        if (booking.getTotalPrice() != null) {
            User user = booking.getUser();

            // Tính điểm: 1000 VND = 1 điểm (Lấy phần nguyên)
            int earnedPoints = booking.getTotalPrice().divide(BigDecimal.valueOf(1000)).intValue();

            if (earnedPoints > 0) {
                // Cộng điểm vào tổng điểm hiện tại
                int currentPoints = user.getPoints(); // Đảm bảo User entity đã có field này (mặc định 0)
                int newTotalPoints = currentPoints + earnedPoints;
                user.setPoints(newTotalPoints);

                // Cập nhật hạng thành viên dựa trên điểm mới
                updateUserRank(user, newTotalPoints);

                // Lưu thông tin User mới
                userRepo.save(user);
            }
        }

        bookingRepo.save(booking);

        try {
            // Lấy email và tên người nhận (Ưu tiên thông tin Customer trong booking)
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
            String propertyName = booking.getProperty().getPropertyName();

            // Gọi service gửi email
            emailService.sendThankYouEmail(
                    emailTo,
                    nameTo,
                    String.valueOf(booking.getBookingId()),
                    propertyName
            );
            logger.info("✅ Đã gửi email cảm ơn checkout cho Booking ID: {}", bookingId);
        } catch (Exception e) {
            // Log lỗi nhưng không chặn transaction checkout (để khách vẫn checkout được dù lỗi mail)
            logger.error("❌ Lỗi gửi mail cảm ơn sau checkout: {}", e.getMessage());
        }
    }

    // ============================================================
    // 🔥 1. HÀM STATIC: TÍNH RANK TỪ ĐIỂM (DÙNG CHUNG TOÀN HỆ THỐNG)
    // ============================================================
    public static MembershipRank calculateRankFromPoints(int points) {
        if (points >= 10000) {
            return MembershipRank.DIAMOND;
        } else if (points >= 5000) {
            return MembershipRank.GOLD;
        } else if (points >= 1000) {
            return MembershipRank.SILVER;
        } else {
            return MembershipRank.BRONZE;
        }
    }

    // ============================================================
    // 🔥 2. HÀM UPDATE RANK CHO USER (SỬ DỤNG HÀM STATIC Ở TRÊN)
    // ============================================================
    public void updateUserRank(User user, int points) {
        // Tái sử dụng logic tính toán để tránh lặp code
        MembershipRank newRank = calculateRankFromPoints(points);
        user.setMembershipRank(newRank);
    }


    // ================================
    // GET APIs
    // ================================
    public BookingResponseDTO getBookingById(int bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        return convertToDTO(b);
    }

    public List<BookingResponseDTO> getBookingsByUserId(String userId) {
        return bookingRepo.findByUserUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingsByPropertyId(int propertyId) {
        return bookingRepo.findByPropertyPropertyId(propertyId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ================================
    // helper convert
    // ================================
    private BookingResponseDTO convertToDTO(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();

        // 1. Payment Info (Lấy từ bảng Payment)
        Payment payment = paymentRepo.findByBooking_BookingId(b.getBookingId()).orElse(null);
        if (payment != null) {
            dto.setPaymentStatus(payment.getPaymentStatus().name());
            // ✅ Map thêm Payment Method (Ví dụ: Momo, VNPay...)
            dto.setPaymentMethod(payment.getPaymentMethod());
        }

        // 2. Basic Info
        dto.setBookingId(b.getBookingId());
        dto.setPropertyId(b.getProperty().getPropertyId());
        dto.setRoomId(b.getRoom() != null ? b.getRoom().getRoomId() : null);

        // 3. Date & Money
        dto.setCheckInDate(b.getCheckInDate());
        dto.setCheckOutDate(b.getCheckOutDate());
        dto.setGuestCount(b.getGuestCount());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setPenaltyAmount(b.getPenaltyAmount());
        dto.setRefundAmount(b.getRefundAmount());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setAdminPromotionCode(b.getAdminPromotionCode());
        dto.setOwnerPromotionCode(b.getPromotionCode());
        dto.setReviewed(ratingRepository.existsByBookingId_BookingId(b.getBookingId()));
        dto.setDiscountAmount(b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO);
        dto.setPromotionCode(b.getPromotionCode());
        dto.setStatus(b.getStatus());

        // ✅ Map thêm Special Request
        dto.setSpecialRequest(b.getSpecialRequest());

        // 4. Map Property Info (Ảnh bìa, địa chỉ...)
        if (b.getProperty() != null) {
            dto.setPropertyName(b.getProperty().getPropertyName());
            dto.setPropertyAddress(b.getProperty().getAddress() + ", " + b.getProperty().getCity());

            String coverUrl = null;
            if (b.getProperty().getImages() != null && !b.getProperty().getImages().isEmpty()) {
                coverUrl = b.getProperty().getImages().stream()
                        .filter(PropertyImage::isCover)
                        .findFirst()
                        .map(PropertyImage::getImageUrl)
                        .orElse(null);

                if (coverUrl == null) {
                    coverUrl = b.getProperty().getImages().get(0).getImageUrl();
                }
            }
            dto.setPropertyImage(coverUrl);
        }

        // 5. Map Room Info
        if (b.getRoom() != null) {
            dto.setRoomName(b.getRoom().getRoomName());
        }

        // ✅ 6. MAP USER INFO (QUAN TRỌNG NHẤT)
        // Logic: Ưu tiên lấy thông tin "Customer" lưu trong Booking (người ở thực tế).
        // Nếu Booking cũ chưa có, mới lấy từ User Account.

        String finalName = b.getCustomerName();
        String finalEmail = b.getCustomerEmail();
        String finalPhone = b.getCustomerPhone();

        // Fallback: Nếu trong Booking null thì lấy từ User gốc
        if (b.getUser() != null) {
            if (finalName == null) finalName = b.getUser().getFullName();
            if (finalEmail == null) finalEmail = b.getUser().getEmail();
            if (finalPhone == null) finalPhone = b.getUser().getPhoneNumber();
        }

        BookingResponseDTO.UserSummaryDto userDto = new BookingResponseDTO.UserSummaryDto(
                b.getUser() != null ? b.getUser().getUserId() : null,
                finalName,  // Tên người ở
                finalEmail, // Email người ở
                finalPhone  // SĐT người ở
        );
        dto.setUser(userDto);

        return dto;
    }

    // ================================
    // 🔍 LẤY LỊCH BẬN CỦA PHÒNG
    // ================================
    public List<Map<String, String>> getRoomAvailability(int roomId) {
        // Lấy tất cả booking từ ngày hôm nay trở đi
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepo.findFutureBookingsByRoomId(roomId, today);

        // Chuyển đổi sang List Map đơn giản: [{start: "2023-12-01", end: "2023-12-05"}, ...]
        return bookings.stream().map(b -> {
            Map<String, String> range = new HashMap<>();
            range.put("start", b.getCheckInDate().toString());
            range.put("end", b.getCheckOutDate().toString());
            return range;
        }).collect(Collectors.toList());
    }

    // ============================================================
    // 🔥 HÀM HELPER: TÍNH GIÁ GỐC THEO TỪNG ĐÊM (Có phân biệt cuối tuần)
    // Dùng cho logic áp dụng/gỡ mã giảm giá
    // ============================================================
// Tìm hàm này ở gần cuối file BookingService.java
    private BigDecimal calculateBasePriceForBooking(Booking booking) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate currentDate = booking.getCheckInDate();

        // 1. Lấy giá ngày thường
        BigDecimal weekdayPrice = booking.getRoom().getPricePerNight();

        // 2. 🔥 FIX: Lấy giá cuối tuần từ Room Entity
        BigDecimal weekendPrice = booking.getRoom().getWeekendPrice() != null
                ? booking.getRoom().getWeekendPrice()
                : weekdayPrice;

        while (currentDate.isBefore(booking.getCheckOutDate())) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            if (isWeekend) {
                total = total.add(weekendPrice); // Cộng đúng 400k
            } else {
                total = total.add(weekdayPrice); // Cộng 200k
            }

            currentDate = currentDate.plusDays(1);
        }
        return total; // Kết quả sẽ chuẩn 800k
    }


    // =====================================================
    // ÁP DỤNG MÃ GIẢM GIÁ
    // =====================================================
    @Transactional
    public BookingResponseDTO applyPromotion(int bookingId, String code) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ có thể áp dụng mã cho đơn hàng chưa thanh toán.");
        }

        // 1. Tìm thông tin mã khuyến mãi mới
        Promotion newPromo = findApplicablePromotion(code, booking.getProperty().getPropertyId());
        if (newPromo == null) {
            throw new RuntimeException("Mã giảm giá không hợp lệ hoặc không áp dụng cho khách sạn này.");
        }

        // 2. Xác định thứ tự ưu tiên (Strict Order: Mã cũ trước, Mã mới sau)
        Promotion ownerPromo = null;
        Promotion adminPromo = null;
        boolean isOwnerFirst = true; // Mặc định Owner trước nếu chưa có gì

        if (newPromo.getProperty() != null) {
            // >>> Mã mới là OWNER
            booking.setPromotionCode(code.toUpperCase());
            ownerPromo = newPromo;

            // Kiểm tra: Đã có mã Admin từ trước chưa?
            if (booking.getAdminPromotionCode() != null) {
                // Đã có Admin => Admin (Cũ) tính trước, Owner (Mới) tính sau
                isOwnerFirst = false;
                // Load lại mã Admin cũ
                List<Promotion> promos = promotionRepo.findValidPromotionsList(booking.getAdminPromotionCode(), LocalDateTime.now());
                adminPromo = promos.stream().filter(p -> p.getProperty() == null).findFirst().orElse(null);
            }
        } else {
            // >>> Mã mới là ADMIN
            booking.setAdminPromotionCode(code.toUpperCase());
            adminPromo = newPromo;

            // Kiểm tra: Đã có mã Owner từ trước chưa?
            if (booking.getPromotionCode() != null) {
                // Đã có Owner => Owner (Cũ) tính trước, Admin (Mới) tính sau
                isOwnerFirst = true;
                // Load lại mã Owner cũ
                ownerPromo = findApplicablePromotion(booking.getPromotionCode(), booking.getProperty().getPropertyId());
            }
        }

        // 3. Tính toán giá theo thứ tự ĐÃ XÁC ĐỊNH
        calculatePriceStrictOrder(booking, ownerPromo, adminPromo, isOwnerFirst);

        // 4. Lưu & Đồng bộ
        Booking saved = bookingRepo.saveAndFlush(booking);
        updatePaymentAmount(saved);

        return convertToDTO(saved);
    }
    // Hàm tính giá theo thứ tự cứng (Strict Order)
    private void calculatePriceStrictOrder(Booking booking, Promotion ownerPromo, Promotion adminPromo, boolean isOwnerFirst) {
        // 1. Tính giá gốc
        // 🔥 ĐÃ SỬA LỖI: Sử dụng hàm Helper để tính giá gốc chính xác theo đêm/cuối tuần
        BigDecimal basePrice = calculateBasePriceForBooking(booking);

        BigDecimal finalPrice;

        // 2. Tính toán dựa trên thứ tự
        // Hàm calculateSequence(startPrice, first, second) đã có sẵn logic:
        // - Áp dụng first trên startPrice
        // - Áp dụng second trên giá còn lại
        if (isOwnerFirst) {
            // Kịch bản: Owner trước -> Admin sau
            finalPrice = calculateSequence(basePrice, ownerPromo, adminPromo);
        } else {
            // Kịch bản: Admin trước -> Owner sau
            finalPrice = calculateSequence(basePrice, adminPromo, ownerPromo);
        }

        // 3. Set vào Booking
        booking.setTotalPrice(finalPrice);
        booking.setDiscountAmount(basePrice.subtract(finalPrice));

        System.out.println("✅ Calculated Strict Order (" + (isOwnerFirst ? "Owner->Admin" : "Admin->Owner") + "): " + finalPrice);
    }
    // Hàm phụ trợ để tính theo chuỗi: Price -> Promo1 -> Promo2
    private BigDecimal calculateSequence(BigDecimal startPrice, Promotion first, Promotion second) {
        BigDecimal currentPrice = startPrice;

        // Áp dụng mã thứ nhất
        // [FIX]: Dùng startPrice (giá gốc) để check điều kiện min amount
        if (first != null && checkMinAmount(first, startPrice)) {
            BigDecimal discount = calculateDiscount(currentPrice, first);
            currentPrice = currentPrice.subtract(discount);
            if (currentPrice.compareTo(BigDecimal.ZERO) < 0) currentPrice = BigDecimal.ZERO;
        }

        // Áp dụng mã thứ hai
        // [FIX QUAN TRỌNG]: Phải dùng startPrice (giá gốc) để check điều kiện min amount
        // Code cũ dùng 'currentPrice' (giá đã giảm) -> Dễ bị trượt điều kiện minAmount của Owner
        if (second != null && checkMinAmount(second, startPrice)) {
            BigDecimal discount = calculateDiscount(currentPrice, second);
            currentPrice = currentPrice.subtract(discount);
            if (currentPrice.compareTo(BigDecimal.ZERO) < 0) currentPrice = BigDecimal.ZERO;
        }

        return currentPrice;
    }
    @Transactional
    public BookingResponseDTO removePromotion(int bookingId, String codeToRemove) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ được bỏ mã với đơn chưa thanh toán.");
        }

        // 1. Xóa mã khỏi entity
        boolean isOwnerCode = codeToRemove.equalsIgnoreCase(booking.getPromotionCode());
        boolean isAdminCode = codeToRemove.equalsIgnoreCase(booking.getAdminPromotionCode());

        if (isOwnerCode) booking.setPromotionCode(null);
        if (isAdminCode) booking.setAdminPromotionCode(null);

        if (!isOwnerCode && !isAdminCode) {
            throw new RuntimeException("Mã này không có trong đơn hàng.");
        }

        // 2. Load lại mã còn lại (nếu có)
        Promotion ownerPromo = null;
        if (booking.getPromotionCode() != null) {
            ownerPromo = findApplicablePromotion(booking.getPromotionCode(), booking.getProperty().getPropertyId());
        }

        Promotion adminPromo = null;
        if (booking.getAdminPromotionCode() != null) {
            List<Promotion> promos = promotionRepo.findValidPromotionsList(booking.getAdminPromotionCode(), LocalDateTime.now());
            adminPromo = promos.stream().filter(p -> p.getProperty() == null).findFirst().orElse(null);
        }

        // 3. Tính lại tiền
        // Khi chỉ còn 1 mã (hoặc 0), thứ tự "ai trước ai sau" không quan trọng
        // vì calculateSequence sẽ tự bỏ qua tham số null. Ta để mặc định true.
        calculatePriceStrictOrder(booking, ownerPromo, adminPromo, true);

        Booking saved = bookingRepo.saveAndFlush(booking);
        updatePaymentAmount(saved);
        return convertToDTO(saved);
    }

    // --- Helper: Tính tiền giảm ---
    private BigDecimal calculateDiscount(BigDecimal amountToApply, Promotion promo) {
        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = promo.getDiscountValue();
        } else {
            // Giảm theo %
            discount = amountToApply.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100));

            // [FIX LỖI TẠI ĐÂY]: Check Max Discount
            // Logic cũ: so sánh > maxDiscountAmount. Nếu max = 0 (không giới hạn) thì discount > 0 -> bị gán về 0.
            // Logic mới: Chỉ áp dụng trần (cap) khi maxDiscountAmount > 0.
            if (promo.getMaxDiscountAmount() != null
                    && promo.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                    && discount.compareTo(promo.getMaxDiscountAmount()) > 0) {

                discount = promo.getMaxDiscountAmount();
            }
        }
        // Không giảm quá số tiền hiện tại
        return discount.compareTo(amountToApply) > 0 ? amountToApply : discount;
    }

    private boolean checkMinAmount(Promotion promo, BigDecimal amount) {
        return promo.getMinBookingAmount() == null || amount.compareTo(promo.getMinBookingAmount()) >= 0;
    }

    private void updatePaymentAmount(Booking booking) {
        Payment payment = paymentRepo.findByBooking_BookingId(booking.getBookingId()).orElse(null);
        if (payment != null) {
            // Cách 1: Chỉ set giá trị và save, nhưng đảm bảo Payment entity được load trong cùng Transaction
            // để nó nhận diện được Booking instance mới nhất.
            // Do chúng ta đang dùng @Transactional ở hàm cha, nên payment.getBooking()
            // VỀ LÝ THUYẾT sẽ trỏ cùng 1 instance với 'booking'.

            // Tuy nhiên, để an toàn tuyệt đối, ta set lại TotalAmount thủ công
            payment.setTotalAmount(booking.getTotalPrice());
            paymentRepo.save(payment);

            logger.info("✅ Đã cập nhật Payment amount thành: {}", booking.getTotalPrice());
        }
    }

    // ============================================================
    // 🔥 [NEW] XỬ LÝ THANH TOÁN THÀNH CÔNG (Được gọi từ PaymentController/Service)
    // ============================================================
    @Transactional // Nên thêm Transactional nếu chưa có ở class level
    public void confirmBookingPayment(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 1. Kiểm tra trạng thái hợp lệ
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            logger.warn("Booking {} đã được xử lý trước đó. Trạng thái hiện tại: {}", bookingId, booking.getStatus());
            return;
        }

        // 2. Cập nhật trạng thái Booking -> CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);

        // 3. Cập nhật trạng thái Payment -> PAID/APPROVED
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
        if (payment != null) {
            payment.setPaymentStatus(PaymentStatus.APPROVED);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepo.save(payment);
        }

        bookingRepo.save(booking);
        logger.info("✅ Booking {} đã được xác nhận thanh toán thành công.", bookingId);

        // 4. Gửi email xác nhận đặt phòng (Giữ nguyên logic cũ)
        try {
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
            emailService.sendBookingConfirmationEmail(emailTo, nameTo, String.valueOf(bookingId));
        } catch (Exception e) {
            logger.error("Lỗi gửi email xác nhận booking: {}", e.getMessage());
        }

        // ========================================================================
        // 🔥 [CẬP NHẬT] GỬI THÔNG BÁO (NOTIFICATION)
        // ========================================================================
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String propertyName = booking.getProperty().getPropertyName();
            String priceFormatted = String.format("%,.0f", booking.getTotalPrice());

            // A. Gửi cho OWNER (Người nhận tiền/đơn)
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(), // [SỬA] Lấy String ID
                        "Thanh toán thành công #" + booking.getBookingId(),
                        "Khách hàng " + booking.getCustomerName() + " đã thanh toán " + priceFormatted + " VNĐ. Đơn hàng đã được xác nhận!",
                        NotificationType.BOOKING_RECEIVED, // [SỬA] Dùng Type của Owner
                        relatedId
                );
            }

            // B. [MỚI] Gửi cho CUSTOMER (Người đặt)
            // Đây là thông báo quan trọng nhất để khách biết mình đã có phòng
            User customer = booking.getUser();
            if (customer != null) {
                notificationService.sendNotification(
                        customer.getUserId(), // [SỬA] Lấy String ID
                        "Đặt phòng thành công!",
                        "Chúc mừng! Đơn phòng #" + booking.getBookingId() + " tại " + propertyName + " đã được xác nhận. Chúc bạn có kỳ nghỉ vui vẻ!",
                        NotificationType.BOOKING_SUCCESS, // [SỬA] Dùng Type của Customer (Màu xanh)
                        relatedId
                );
            }

        } catch (Exception e) {
            logger.error("Lỗi gửi thông báo thanh toán: {}", e.getMessage());
        }

        // 5. Kiểm tra gửi mail nhắc nhở check-in ngay lập tức (Giữ nguyên)
        checkAndSendImmediateReminder(booking);
    }

    // ============================================================
    // 🔥 SCHEDULER: QUÉT VÀ GỬI EMAIL NHẮC CHECK-IN (Chạy 8h sáng hàng ngày)
    // ============================================================
    @Transactional
    public void sendCheckinReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Tìm các đơn CONFIRMED check-in ngày mai
        List<Booking> bookings = bookingRepo.findByCheckInDateAndStatus(tomorrow, BookingStatus.CONFIRMED);

        if (!bookings.isEmpty()) {
            logger.info("🔍 Tìm thấy {} đơn hàng check-in ngày mai ({})", bookings.size(), tomorrow);

            for (Booking booking : bookings) {
                try {
                    String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                    String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
                    String propertyName = booking.getProperty().getPropertyName();

                    emailService.sendCheckinReminderEmail(
                            emailTo,
                            nameTo,
                            String.valueOf(booking.getBookingId()),
                            propertyName,
                            booking.getCheckInDate().toString()
                    );
                    logger.info("✅ Đã gửi email nhắc check-in (Scheduler) cho Booking ID: {}", booking.getBookingId());

                } catch (Exception e) {
                    logger.error("❌ Lỗi gửi email nhắc check-in (Scheduler) cho ID {}: {}", booking.getBookingId(), e.getMessage());
                }
            }
        } else {
            logger.info("📅 Không có đơn hàng nào check-in vào ngày mai ({})", tomorrow);
        }
    }

    // ============================================================
    // 🔥 HELPER: KIỂM TRA VÀ GỬI MAIL NHẮC CHECK-IN NGAY (Real-time)
    // ============================================================
    public void checkAndSendImmediateReminder(Booking booking) {
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Logic: Nếu khách đặt phòng Check-in là HÔM NAY hoặc NGÀY MAI
        // thì gửi email nhắc nhở ngay lập tức (vì đã lỡ hoặc sắp tới giờ Scheduler chạy)
        if (checkInDate.isEqual(today) || checkInDate.isEqual(tomorrow)) {
            try {
                String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
                String propertyName = booking.getProperty().getPropertyName();

                emailService.sendCheckinReminderEmail(
                        emailTo,
                        nameTo,
                        String.valueOf(booking.getBookingId()),
                        propertyName,
                        booking.getCheckInDate().toString()
                );
                logger.info("⚡ Đã gửi email nhắc nhở NGAY LẬP TỨC cho Booking ID: {}", booking.getBookingId());
            } catch (Exception e) {
                logger.error("❌ Lỗi gửi email nhắc nhở ngay lập tức: {}", e.getMessage());
            }
        }
    }

    // Hàm helper mới: Tìm đúng mã khuyến mãi khớp với khách sạn hoặc là mã Admin
    private Promotion findApplicablePromotion(String code, Integer propertyId) {
        // Sử dụng findValidPromotionsList thay vì findValidPromotion (dựa trên PromotionService đã có)
        List<Promotion> promotions = promotionRepo.findValidPromotionsList(code.toUpperCase(), LocalDateTime.now());

        if (promotions.isEmpty()) {
            return null;
        }

        // 1. Ưu tiên tìm mã khớp chính xác Property ID
        Promotion specificPromo = promotions.stream()
                .filter(p -> p.getProperty() != null && p.getProperty().getPropertyId() == propertyId)
                .findFirst()
                .orElse(null);

        if (specificPromo != null) return specificPromo;

        // 2. Nếu không có mã riêng, tìm mã Global (Admin)
        return promotions.stream()
                .filter(p -> p.getProperty() == null)
                .findFirst()
                .orElse(null);
    }
}