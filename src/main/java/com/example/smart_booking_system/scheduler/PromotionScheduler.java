package com.example.smart_booking_system.scheduler;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromotionScheduler {

    private final PromotionRepository promotionRepository;

    /**
     * ✅ ĐÃ SỬA: Chạy định kỳ MỖI PHÚT (vào giây thứ 0)
     * Cron: "0 * * * * ?" nghĩa là chạy mỗi phút một lần.
     * Như vậy nếu mã hết hạn lúc 14:02, thì chậm nhất 14:03 hệ thống sẽ quét thấy và update.
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void autoExpirePromotions() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm các mã đang ACTIVE mà thời gian kết thúc < thời gian hiện tại (so sánh cả giờ phút giây)
        List<Promotion> expiredPromotions = promotionRepository.findByStatusAndEndDateBefore(
                PromotionStatus.ACTIVE,
                now
        );

        if (!expiredPromotions.isEmpty()) {
            // 2. Cập nhật trạng thái sang EXPIRED
            for (Promotion p : expiredPromotions) {
                p.setStatus(PromotionStatus.EXPIRED);
            }

            // 3. Lưu lại vào DB
            promotionRepository.saveAll(expiredPromotions);

            System.out.println("PromotionScheduler: Đã hết hạn " + expiredPromotions.size() + " mã tại thời điểm " + now);
        }
    }
}