package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.PropertyAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, Integer> {

    List<PropertyAmenity> findByProperty_PropertyId(int propertyId);

    // CHECK EXISTS
    @Query("""
        SELECT COUNT(pa) > 0 
        FROM PropertyAmenity pa
        WHERE pa.property.propertyId = :propertyId 
          AND pa.amenity.amenityId = :amenityId
          AND pa.active = true
    """)
    boolean existsByPropertyAndAmenity(
            @Param("propertyId") int propertyId,
            @Param("amenityId") int amenityId
    );



    // GET 1 bản ghi
    @Query("""
        SELECT pa 
        FROM PropertyAmenity pa
        WHERE pa.property.propertyId = :propertyId
          AND pa.amenity.amenityId = :amenityId
          AND pa.active = true
    """)
    PropertyAmenity findByPropertyAndAmenity(
            @Param("propertyId") int propertyId,
            @Param("amenityId") int amenityId
    );



    // GET TẤT CẢ ACTIVE CỦA PROPERTY
    @Query("""
        SELECT pa
        FROM PropertyAmenity pa
        WHERE pa.property.propertyId = :propertyId
          AND pa.active = true
    """)
    List<PropertyAmenity> findActiveByProperty(@Param("propertyId") int propertyId);

}
