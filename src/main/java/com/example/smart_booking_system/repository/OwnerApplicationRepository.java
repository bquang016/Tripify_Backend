package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerApplicationRepository extends JpaRepository<OwnerApplication, Long> {

    /**
     * Checks if an application exists for a given email with a specific status.
     *
     * @param email  The email to check.
     * @param status The status to check for.
     * @return true if an application exists, false otherwise.
     */
    boolean existsByEmailAndStatus(String email, ApplicationStatus status);

    /**
     * Finds an owner application by email.
     *
     * @param email The email to search for.
     * @return an Optional containing the OwnerApplication if found.
     */
    Optional<OwnerApplication> findByEmail(String email);
}
