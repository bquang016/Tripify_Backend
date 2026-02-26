package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.SystemLog;
import com.example.smart_booking_system.enums.LogEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {

    //Log theo entity (PROPERTY / ROOM)
    @Query("""
        SELECT l
        FROM SystemLog l
        WHERE l.entityType = :entityType
          AND l.entityId = :entityId
        ORDER BY l.createdAt DESC
    """)
    Page<SystemLog> findByEntity(
            @Param("entityType") LogEntityType entityType,
            @Param("entityId") String entityId,
            Pageable pageable
    );

    //Log theo người thực hiện
    @Query("""
        SELECT l
        FROM SystemLog l
        WHERE l.actor.userId = :userId
        ORDER BY l.createdAt DESC
    """)
    Page<SystemLog> findByActor(
            @Param("userId") String userId,
            Pageable pageable
    );

    //Log theo entityType
    @Query("""
        SELECT l
        FROM SystemLog l
        WHERE l.entityType = :entityType
        ORDER BY l.createdAt DESC
    """)
    Page<SystemLog> findByEntityType(
            @Param("entityType") LogEntityType entityType,
            Pageable pageable
    );

    //Admin xem toàn bộ log
    @Query("""
        SELECT l
        FROM SystemLog l
        ORDER BY l.createdAt DESC
    """)
    Page<SystemLog> findAllLogs(Pageable pageable);
}
