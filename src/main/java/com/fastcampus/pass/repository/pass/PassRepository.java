package com.fastcampus.pass.repository.pass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PassRepository extends JpaRepository<PassEntity, Integer> {
    @Query("select p" +
            " from PassEntity p" +
            " where p.status = :status and" +
            " p.endedAt <= :endedAt")
    List<PassEntity> findAllByStatusAAndEndedAt(@Param("status") PassStatus status, @Param("endedAt") LocalDateTime endedAt);
}
