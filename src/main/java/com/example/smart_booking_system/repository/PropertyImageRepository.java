package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Integer> {
    List<PropertyImage> findByProperty_PropertyId(int propertyId);

    @Query("""
        SELECT pi FROM PropertyImage pi
        WHERE pi.property.propertyId = :propertyId
        AND pi.isActive = true
    """)
    List<PropertyImage> findActiveImagesByPropertyId(int propertyId);
    Optional<PropertyImage> findFirstByProperty_PropertyIdAndIsCoverTrue(int propertyId);
    Optional<PropertyImage> findFirstByProperty_PropertyId(int propertyId);
    @Modifying
    @Query("UPDATE PropertyImage p SET p.isCover = false WHERE p.property.propertyId = :propertyId")
    void resetCoverImageByPropertyId(@Param("propertyId") int propertyId);
}
