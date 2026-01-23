package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnerApplicationRepository extends JpaRepository<OwnerApplication, Long> {

    @Query("SELECT oa FROM OwnerApplication oa JOIN FETCH oa.userId WHERE oa.status = :status")
    List<OwnerApplication> findByStatusWithApplicant(ApplicationStatus status);
}