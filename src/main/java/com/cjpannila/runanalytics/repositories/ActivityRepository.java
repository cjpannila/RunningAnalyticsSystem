package com.cjpannila.runanalytics.repositories;

import com.cjpannila.runanalytics.entities.Activity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRepository extends CrudRepository<Activity, Long> {
    List<Activity> findByUser_UserId(Long userId);

    java.util.Optional<Activity> findTopByUser_UserIdOrderByStartTimeDesc(Long userId);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.startTime IS NOT NULL
              AND a.startTime >= :weekStart
              AND a.startTime < :weekEndExclusive
              AND LOWER(COALESCE(a.activityType, '')) LIKE '%run%'
              AND EXISTS (
                  SELECT 1 FROM UserClub uc
                  WHERE uc.user.userId = a.user.userId
                    AND uc.club.clubId = :clubId
              )
            """)
    List<Activity> findClubRunActivitiesForWeek(@Param("clubId") Long clubId,
                                                @Param("weekStart") LocalDateTime weekStart,
                                                @Param("weekEndExclusive") LocalDateTime weekEndExclusive);

    @Query("""
            SELECT a
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.startTime BETWEEN :from AND :to
            """)
    List<Activity> findRunningActivitiesForDateRange(
            Long userId,
            LocalDateTime from,
            LocalDateTime to);

    @Query("""
            SELECT AVG(a.avgHeartrateBpm)
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.avgHeartrateBpm IS NOT NULL
            AND a.startTime BETWEEN :from AND :to
            """)
    Double getAverageHeartRateForDateRange(
            Long userId,
            LocalDateTime from,
            LocalDateTime to);
}

