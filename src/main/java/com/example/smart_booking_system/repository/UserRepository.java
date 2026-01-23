package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.smart_booking_system.enums.MembershipRank;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.smart_booking_system.dto.response.admin.OwnerSelectDTO;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by verification token
     */
    Optional<User> findByVerificationToken(String token);

    /**
     * Find user by reset password token
     */
    Optional<User> findByResetPasswordToken(String token);

    /**
     * Find users by status
     */
    List<User> findByStatus(String status);

    /**
     * Find users with specific role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Delete expired verification tokens
     */
    @Query("UPDATE User u SET u.verificationToken = NULL, u.verificationTokenExpiry = NULL " +
            "WHERE u.verificationTokenExpiry < :now")
    void deleteExpiredVerificationTokens(@Param("now") LocalDateTime now);

    /**
     * Delete expired reset password tokens
     */
    @Query("UPDATE User u SET u.resetPasswordToken = NULL, u.resetPasswordTokenExpiry = NULL " +
            "WHERE u.resetPasswordTokenExpiry < :now")
    void deleteExpiredResetPasswordTokens(@Param("now") LocalDateTime now);

    /**
     * Count users by status
     */
    long countByStatus(String status);

    /**
     * Search users by full name or email
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    // --- QUERY MỚI CHO ADMIN ---
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.roles r " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "       LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:role IS NULL OR :role = '' OR r.roleName = :role) " +
            "AND (:status IS NULL OR :status = '' OR u.status = :status) " +
            "AND NOT EXISTS (SELECT subR FROM u.roles subR WHERE subR.roleName = 'ADMIN') " +
            "AND (:rank IS NULL OR u.membershipRank = :rank)")
    Page<User> findUsersWithFilter(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("status") String status,
            @Param("rank") MembershipRank rank, // Đã thêm tham số này
            Pageable pageable
    );
    @Query("SELECT FUNCTION('MONTH', u.createdAt) as month, COUNT(u) as count " +
            "FROM User u " +
            "WHERE FUNCTION('YEAR', u.createdAt) = :year " +
            "GROUP BY FUNCTION('MONTH', u.createdAt) " +
            "ORDER BY FUNCTION('MONTH', u.createdAt) ASC")
    List<Object[]> getMonthlyUserGrowth(@Param("year") int year);
    @Query("""
  SELECT new com.example.smart_booking_system.dto.response.admin.OwnerSelectDTO(
      u.userId, u.fullName, u.email
  )
  FROM User u
  JOIN u.roles r
  WHERE r.roleName = 'OWNER'
  ORDER BY u.fullName
""")
    List<OwnerSelectDTO> findOwnersForDashboard();


}

