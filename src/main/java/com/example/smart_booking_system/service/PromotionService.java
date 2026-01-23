package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.MembershipRank;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ForbiddenException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PromotionRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final FileStorageService fileStorageService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AuthService authService;
    private final BookingService bookingService;

    // 1. TẠO MỚI
    public PromotionResponseDTO createPromotion(PromotionRequestDTO req) {
        User currentUser = authService.getCurrentUser();


        if (req.getPropertyId() == null) {
            // ADMIN: Không được trùng với bất kỳ mã nào ĐANG HOẠT ĐỘNG
            if (promotionRepository.existsByCodeAndStatusNot(req.getCode(), PromotionStatus.DELETED)) {
                throw new BadRequestException("Mã khuyến mãi '" + req.getCode() + "' đang được sử dụng (chưa xóa). Vui lòng chọn mã khác.");
            }
        } else {
            // OWNER:
            // 1. Check trùng Admin (đang hoạt động)
            if (promotionRepository.existsByCodeAndPropertyIsNullAndStatusNot(req.getCode(), PromotionStatus.DELETED)) {
                throw new BadRequestException("Mã '" + req.getCode() + "' đang được Admin sử dụng.");
            }
            // 2. Check trùng nội bộ KS (đang hoạt động)
            if (promotionRepository.existsByCodeAndProperty_PropertyIdAndStatusNot(req.getCode(), req.getPropertyId(), PromotionStatus.DELETED)) {
                throw new BadRequestException("Mã '" + req.getCode() + "' đang hoạt động tại khách sạn này.");
            }
        }

        if (req.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ngày bắt đầu không được chọn trong quá khứ.");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu.");
        }

        Promotion p = new Promotion();
        p.setCode(req.getCode().toUpperCase());
        p.setDescription(req.getDescription());
        p.setDiscountType(req.getDiscountType());
        p.setDiscountValue(req.getDiscountValue());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMinBookingAmount(req.getMinBookingAmount());
        p.setMaxDiscountAmount(req.getMaxDiscountAmount());
        p.setMinMembershipRank(req.getMinMembershipRank());
        p.setUsageLimit(req.getUsageLimit());
        p.setUsageCount(0);

        if (req.getPropertyId() != null) {
            Property property = propertyRepository.findById(req.getPropertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

            boolean isOwner = property.getOwner().getUserId().equals(currentUser.getUserId());
            boolean isAdmin = currentUser.hasRole("ADMIN");

            if (!isOwner && !isAdmin) {
                throw new ForbiddenException("Bạn không có quyền tạo khuyến mãi cho tài sản này.");
            }
            p.setProperty(property);
        } else {
            if (!currentUser.hasRole("ADMIN")) {
                throw new ForbiddenException("Chỉ Admin mới được tạo mã khuyến mãi toàn sàn.");
            }
            p.setProperty(null);
        }

        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            p.checkAndSetStatus();
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 2. CẬP NHẬT
    public PromotionResponseDTO updatePromotion(int id, PromotionRequestDTO req) {
        User currentUser = authService.getCurrentUser();
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        if (p.getStatus() == PromotionStatus.DELETED) throw new BadRequestException("Không thể sửa mã đã xóa.");

        boolean isCodeChanged = !p.getCode().equalsIgnoreCase(req.getCode());
        Integer currentPropertyId = p.getProperty() != null ? p.getProperty().getPropertyId() : null;
        Integer newPropertyId = req.getPropertyId();

        Integer targetPropertyId = newPropertyId != null ? newPropertyId : currentPropertyId;

        // Check trùng lặp (Bỏ qua DELETED)
        if (isCodeChanged || (newPropertyId != null && !newPropertyId.equals(currentPropertyId))) {
            if (targetPropertyId == null) {
                // Admin Update
                if (promotionRepository.existsByCodeAndStatusNot(req.getCode(), PromotionStatus.DELETED)) {
                    throw new BadRequestException("Mã '" + req.getCode() + "' đang được sử dụng.");
                }
            } else {
                // Owner Update
                if (promotionRepository.existsByCodeAndPropertyIsNullAndStatusNot(req.getCode(), PromotionStatus.DELETED)) {
                    throw new BadRequestException("Mã '" + req.getCode() + "' đang được Admin sử dụng.");
                }
                if (promotionRepository.existsByCodeAndProperty_PropertyIdAndStatusNot(req.getCode(), targetPropertyId, PromotionStatus.DELETED)) {
                    throw new BadRequestException("Mã '" + req.getCode() + "' đã tồn tại tại khách sạn bạn chọn.");
                }
            }
        }

        if (req.getPropertyId() != null) {
            if (p.getProperty() == null) {
                if (!currentUser.hasRole("ADMIN")) throw new ForbiddenException("Không thể sửa mã toàn sàn.");
            } else if (p.getProperty().getPropertyId() != req.getPropertyId()) {
                Property newProperty = propertyRepository.findById(req.getPropertyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
                boolean isOwner = newProperty.getOwner().getUserId().equals(currentUser.getUserId());
                boolean isAdmin = currentUser.hasRole("ADMIN");
                if (!isOwner && !isAdmin) throw new ForbiddenException("Bạn không sở hữu khách sạn mới này.");
                p.setProperty(newProperty);
            }
        }

        p.setCode(req.getCode().toUpperCase());
        p.setDescription(req.getDescription());
        p.setDiscountType(req.getDiscountType());
        p.setDiscountValue(req.getDiscountValue());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMinBookingAmount(req.getMinBookingAmount());
        p.setMaxDiscountAmount(req.getMaxDiscountAmount());
        p.setMinMembershipRank(req.getMinMembershipRank());
        p.setUsageLimit(req.getUsageLimit());

        if (req.getStatus() != null) p.setStatus(req.getStatus());
        else p.checkAndSetStatus();

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    public Promotion validateAndGetPromotionForBooking(String code, Integer propertyId) {
        List<Promotion> promotions = promotionRepository.findValidPromotionsList(code.toUpperCase(), LocalDateTime.now());

        if (promotions.isEmpty()) {
            throw new ResourceNotFoundException("Mã khuyến mãi không hợp lệ hoặc đã hết hạn.");
        }

        // Lọc property
        List<Promotion> applicable = promotions.stream()
                .filter(p -> p.getProperty() == null || p.getProperty().getPropertyId() == propertyId)
                .collect(Collectors.toList());

        if (applicable.isEmpty()) {
            throw new BadRequestException("Mã này không áp dụng cho khách sạn bạn đang đặt.");
        }

        applicable.sort((p1, p2) -> {
            if (p1.getProperty() != null && p2.getProperty() == null) return -1;
            if (p1.getProperty() == null && p2.getProperty() != null) return 1;
            return 0;
        });

        return applicable.get(0);
    }

    public void deletePromotion(int id) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        p.setStatus(PromotionStatus.DELETED);
        promotionRepository.save(p);
    }

    public PromotionResponseDTO toggleStatus(int id) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if(p.getStatus() == PromotionStatus.DELETED) throw new BadRequestException("Cannot toggle deleted promo");
        if (p.getStatus() == PromotionStatus.PAUSED) p.checkAndSetStatus();
        else p.setStatus(PromotionStatus.PAUSED);
        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    public PromotionResponseDTO suggestBestPromotion(String userId, Integer propertyId, BigDecimal bookingAmount) {
        if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("User not found");

        // 1. Tính toán Rank người dùng
        BigDecimal totalSpent = bookingRepository.calculateTotalSpentByUser(userId);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;
        int currentPoints = totalSpent.divide(BigDecimal.valueOf(1000)).intValue();
        MembershipRank userRank = BookingService.calculateRankFromPoints(currentPoints);

        // 2. Lấy danh sách TẤT CẢ khuyến mãi đang chạy (Gộp cả Admin và Owner)
        List<Promotion> allPromotions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 2.1. Luôn lấy mã Global (Admin)
        List<Promotion> adminPromos = promotionRepository.findByPropertyIsNull().stream()
                .filter(p -> p.getStatus() == PromotionStatus.ACTIVE
                        && p.getStartDate().isBefore(now)
                        && p.getEndDate().isAfter(now))
                .collect(Collectors.toList());
        allPromotions.addAll(adminPromos);

        // 2.2. Nếu đang xem phòng cụ thể, lấy thêm mã của Property (Owner)
        if (propertyId != null) {
            List<Promotion> ownerPromos = promotionRepository.findByProperty_PropertyId(propertyId).stream()
                    .filter(p -> p.getStatus() == PromotionStatus.ACTIVE
                            && p.getStartDate().isBefore(now)
                            && p.getEndDate().isAfter(now))
                    .collect(Collectors.toList());
            allPromotions.addAll(ownerPromos);
        }

        // 3. Lọc theo điều kiện & Tìm mã tốt nhất (Discount cao nhất)
        Promotion bestPromotion = allPromotions.stream()
                // Check hạng thành viên
                .filter(p -> isRankEligible(userRank, p.getMinMembershipRank()))
                // Check giá trị đơn tối thiểu
                .filter(p -> p.getMinBookingAmount() == null || bookingAmount.compareTo(p.getMinBookingAmount()) >= 0)
                // Check giới hạn lượt dùng (Optional - nếu bạn muốn kỹ hơn)
                .filter(p -> p.getUsageLimit() == null || p.getUsageCount() < p.getUsageLimit())
                // Sắp xếp: Số tiền giảm giảm dần (Cao nhất lên đầu)
                .sorted(Comparator.comparing((Promotion p) -> calculateDiscountAmount(p, bookingAmount)).reversed())
                .findFirst()
                .orElse(null);

        return bestPromotion != null ? new PromotionResponseDTO(bestPromotion) : null;
    }

    private boolean isRankEligible(MembershipRank userRank, MembershipRank requiredRank) {
        if (requiredRank == null) return true;
        return userRank.ordinal() >= requiredRank.ordinal();
    }

    private BigDecimal calculateDiscountAmount(Promotion p, BigDecimal bookingAmount) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (p.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = p.getDiscountValue();
        } else if (p.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = bookingAmount.multiply(p.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (p.getMaxDiscountAmount() != null && discountAmount.compareTo(p.getMaxDiscountAmount()) > 0) {
                discountAmount = p.getMaxDiscountAmount();
            }
        }
        return discountAmount.min(bookingAmount);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getPromotionsByProperty(int propertyId) {
        return promotionRepository.findByProperty_PropertyId(propertyId).stream()
                .filter(p -> p.getStatus() != PromotionStatus.DELETED)
                .map(PromotionResponseDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllGlobalPromotions() {
        // Thay vì gọi findAll(), gọi hàm mới với JOIN FETCH
        List<Promotion> promotions = promotionRepository.findAllWithProperty();

        return promotions.stream()
                .map(PromotionResponseDTO::new) // PromotionResponseDTO sẽ tự động map PropertyDTO
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO getById(int id) {
        return new PromotionResponseDTO(promotionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found")));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getPromotionsByCurrentOwner() {
        User currentUser = authService.getCurrentUser();
        return promotionRepository.findAllByOwnerId(currentUser.getUserId()).stream()
                .map(PromotionResponseDTO::new)
                .collect(Collectors.toList());
    }

    public PromotionResponseDTO uploadBanner(int promotionId, MultipartFile file) {
        Promotion p = promotionRepository.findById(promotionId).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if (p.getBannerUrl() != null && !p.getBannerUrl().isEmpty()) fileStorageService.deleteFile(p.getBannerUrl());
        p.setBannerUrl(fileStorageService.storeImageFile(file, "campaign-images"));
        return new PromotionResponseDTO(promotionRepository.save(p));
    }

}