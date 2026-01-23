package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.PropertyPolicies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PropertyPoliciesRepository extends JpaRepository<PropertyPolicies, Integer> {

    @Query("""
           SELECT p 
           FROM PropertyPolicies p
           WHERE p.propertyId.propertyId = :propertyId
           """)
    PropertyPolicies findByPropertyId(int propertyId);
}
