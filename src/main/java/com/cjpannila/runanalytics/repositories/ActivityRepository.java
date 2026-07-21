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

    @Query("""
            SELECT AVG(a.avgCadence)
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.avgCadence IS NOT NULL
            AND a.avgCadence > 0
            """)
    Double getAllTimeAverageCadence(@Param("userId") Long userId);

    @Query("""
            SELECT AVG(a.avgHeartrateBpm)
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.avgHeartrateBpm IS NOT NULL
            AND a.avgHeartrateBpm > 0
            """)
    Double getAllTimeAverageHeartRate(@Param("userId") Long userId);

    @Query("""
            SELECT (SUM(a.elevationGainM) / SUM(a.distanceM / 1000.0))
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.elevationGainM IS NOT NULL
            AND a.elevationGainM > 0
            AND a.distanceM IS NOT NULL
            AND a.distanceM > 0
            """)
    Double getAllTimeAverageElevationPerKm(@Param("userId") Long userId);

    @Query("SELECT MIN(a.startTime) FROM Activity a WHERE a.user.userId IN :userIds AND a.activityType = 'Run'")
    LocalDateTime findMinStartTimeForUsers(@Param("userIds") java.util.List<Long> userIds);

    @Query("SELECT MAX(a.startTime) FROM Activity a WHERE a.user.userId IN :userIds AND a.activityType = 'Run'")
    LocalDateTime findMaxStartTimeForUsers(@Param("userIds") java.util.List<Long> userIds);

    @Query("""
            SELECT (COUNT(*))
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.distanceM > 0
            AND a.elevationGainM > 0
            AND a.avgHeartrateBpm > 0
            AND a.avgCadence > 0
            """)
    Integer getRunCountWithAllDataByUser(@Param("userId") Long userId);

    @Query("""
            SELECT (COUNT(*))
            FROM Activity a
            WHERE a.user.userId = :userId
            AND a.activityType = 'Run'
            AND a.distanceM > 0
            """)
    Integer getRunCountByUser(@Param("userId") Long userId);
}

