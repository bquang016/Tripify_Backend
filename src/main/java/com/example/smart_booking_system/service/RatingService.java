package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.RatingRequestDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.entity.RatingImage;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.RatingType;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.RatingImageRepository;
import com.example.smart_booking_system.repository.RatingRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final RatingImageRepository ratingImageRepository;
    private final FileStorageService fileStorageService;
    private final PropertyRepository propertyRepository;

    private static final int PAGE_SIZE = 10;

    private Rating attachImages(Rating rating) {
        if (rating == null) return null;
        List<RatingImage> imgs = ratingImageRepository.getImagesByRatingId(rating.getRatingId());
        rating.setImages(imgs);
        return rating;
    }

    private List<Rating> attachImages(List<Rating> list) {
        list.forEach(this::attachImages);
        return list;
    }

    private RatingType classify(int stars, String comment) {
        if (comment == null) comment = "";
        String lower = comment.toLowerCase();
        if (lower.contains("dm") || lower.contains("địt") || lower.contains("cút")
                || lower.contains("fuck") || lower.contains("shit")
                || lower.contains("bố mày") || lower.contains("óc chó")
                || lower.contains("ngu")|| lower.contains("lol")) {
            return RatingType.VIOLATION;
        }
        if (stars >= 4 || lower.contains("good") || lower.contains("tốt") ||
                lower.contains("great") || lower.contains("tuyệt") || lower.contains("hài lòng")) {
            return RatingType.POSITIVE;
        }
        if (stars <= 2 || lower.contains("bad") || lower.contains("tệ") ||
                lower.contains("kém") || lower.contains("không hài lòng")) {
            return RatingType.NEGATIVE;
        }
        return RatingType.NEGATIVE;
    }

    @Transactional
    public Rating createRating(RatingRequestDTO dto, List<MultipartFile> files) {
        if (ratingRepository.existsByBookingId_BookingId(dto.getBookingId())) {
            throw new RuntimeException("Bạn đã đánh giá đơn đặt phòng này rồi.");
        }

        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá khi chuyến đi đã hoàn thành.");
        }

        RatingType type = classify(dto.getStars(), dto.getComment());
        if (type == RatingType.VIOLATION) throw new RuntimeException("Đánh giá chứa từ ngữ không phù hợp.");

        Rating rating = new Rating();
        rating.setBookingId(booking);
        rating.setUserId(booking.getUser());
        rating.setRating(dto.getStars());
        rating.setComment(dto.getComment());
        rating.setRatingType(type);
        rating.setHidden(false);
        rating.setIsPinned(false);
        rating.setImages(new ArrayList<>());

        Rating savedRating = ratingRepository.save(rating);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {

                // ✅ FIX ĐÚNG LỖI DUY NHẤT: đổi ratingImage → ratingImages
                String fileUrl = fileStorageService.storeImageFile(file, "ratingImages");

                RatingImage img = new RatingImage();
                img.setRating(savedRating);
                img.setImageUrl(fileUrl);
                savedRating.getImages().add(img);
            }
        }

        updatePropertyStats(booking.getProperty().getPropertyId());
        return savedRating;
    }

    @Transactional
    public Rating updateRating(int id, RatingRequestDTO dto, List<MultipartFile> files) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setRating(dto.getStars());
        rating.setComment(dto.getComment());

        RatingType type = classify(dto.getStars(), dto.getComment());
        if (type == RatingType.VIOLATION) throw new RuntimeException("Nội dung không phù hợp.");
        rating.setRatingType(type);

        if (files != null && !files.isEmpty()) {
            if (rating.getImages() == null) {
                rating.setImages(new ArrayList<>());
            }
            rating.getImages().clear();

            for (MultipartFile file : files) {

                // ✅ FIX ĐÚNG CHỖ CẦN SỬA
                String fileUrl = fileStorageService.storeImageFile(file, "ratingImages");

                RatingImage img = new RatingImage();
                img.setRating(rating);
                img.setImageUrl(fileUrl);
                rating.getImages().add(img);
            }
        }

        Rating saved = ratingRepository.save(rating);
        updatePropertyStats(rating.getBookingId().getProperty().getPropertyId());
        return attachImages(saved);
    }

    public void deleteRating(int id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));
        int propertyId = rating.getBookingId().getProperty().getPropertyId();

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String ownerEmail = rating.getUserId().getEmail();

        if (!isAdmin && !currentUsername.equals(ownerEmail)) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này.");
        }

        List<RatingImage> imgs = ratingImageRepository.getImagesByRatingId(id);
        ratingImageRepository.deleteAll(imgs);

        ratingRepository.deleteById(id);
        updatePropertyStats(propertyId);
    }

    private Page<Rating> paginate(List<Rating> list, int page) {
        int start = page * PAGE_SIZE;
        if (start >= list.size()) {
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, PAGE_SIZE), list.size());
        }
        int end = Math.min(start + PAGE_SIZE, list.size());
        List<Rating> content = list.subList(start, end);
        attachImages(content);
        return new PageImpl<>(content, PageRequest.of(page, PAGE_SIZE), list.size());
    }

    public Page<Rating> getRatingsForProperty(int propertyId, int page) {
        List<Rating> all = ratingRepository.getRatingsByProperty(propertyId);
        return paginate(all, page);
    }

    public Rating pinRating(int ratingId, boolean pin) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String currentUserId = "";
        if (auth.getPrincipal() instanceof CustomUserDetails) {
            currentUserId = ((CustomUserDetails) auth.getPrincipal()).getUserId();
        } else {
            throw new RuntimeException("Không xác thực được người dùng.");
        }

        if (!isAdmin) {
            String propertyOwnerId = String.valueOf(rating.getBookingId().getProperty().getOwner().getUserId());
            if (!currentUserId.equals(propertyOwnerId)) {
                throw new RuntimeException("Bạn không có quyền ghim đánh giá của khách sạn này.");
            }
        }

        if (pin) {
            int propertyId = rating.getBookingId().getProperty().getPropertyId();
            int currentPinnedCount = ratingRepository.countByBookingId_Property_PropertyIdAndIsPinnedTrue(propertyId);
            if (currentPinnedCount >= 3) {
                throw new RuntimeException("Chỉ được ghim tối đa 3 bình luận.");
            }
        }

        rating.setIsPinned(pin);
        return attachImages(ratingRepository.save(rating));
    }

    public Rating hideRating(int ratingId, boolean hide) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setHidden(hide);
        Rating saved = ratingRepository.save(rating);

        updatePropertyStats(saved.getBookingId().getProperty().getPropertyId());
        return attachImages(saved);
    }

    private void updatePropertyStats(int propertyId) {
        List<Rating> reviews = ratingRepository.getRatingsByProperty(propertyId);
        int count = reviews.size();
        double avg = 0.0;

        if (count > 0) {
            double sum = reviews.stream().mapToInt(Rating::getRating).sum();
            avg = sum / count;
        }

        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        property.setReviewCount(count);
        property.setRating(avgRating);
        propertyRepository.save(property);
    }

    public Rating getRatingById(int id) {
        Rating rating = ratingRepository.findById(id).orElseThrow(() -> new RuntimeException("Rating not found"));
        if (rating.isHidden()) throw new RuntimeException("This rating is hidden.");
        return attachImages(rating);
    }

    public Page<Rating> getRatingByBooking(int bookingId, int page) {
        return paginate(ratingRepository.getRatingByBookingId(bookingId), page);
    }

    public Page<Rating> getRatingByUser(String userId, int page) {
        return paginate(ratingRepository.getRatingByUserId(userId), page);
    }

    public Page<Rating> getRatingByType(String ratingTypeStr, int page) {
        try {
            RatingType type = RatingType.valueOf(ratingTypeStr.toUpperCase());
            return paginate(ratingRepository.getRatingByRatingType(type), page);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Loại đánh giá không hợp lệ: " + ratingTypeStr);
        }
    }

    public Page<Rating> getHiddenRatings(int page) {
        return paginate(ratingRepository.getRatingByHidden(), page);
    }
}