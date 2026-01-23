package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RatingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RatingImageRepository extends JpaRepository<RatingImage, Integer> {
    @Query(
            value = "SELECT * FROM ratingImages WHERE ratingId = :ratingId",
            nativeQuery = true
    )
    List<RatingImage> getImagesByRatingId(int ratingId);

}
