package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RatingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingImageRepository extends JpaRepository<RatingImage, Integer> {

    // Sử dụng JPQL thay vì Native Query để Hibernate tự động map đúng tên bảng/cột
    @Query("SELECT ri FROM RatingImage ri WHERE ri.rating.ratingId = :ratingId")
    List<RatingImage> getImagesByRatingId(@Param("ratingId") int ratingId);

}