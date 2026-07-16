package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.entities.Activity;
import com.cjpannila.runanalytics.repositories.ActivityRepository;
import com.cjpannila.runanalytics.util.Constants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private final ActivityRepository activityRepository;

    // Calculate avg pace for a user for a given week.
    // Pace is calculated as total moving time (in minutes) divided by total distance (in kilometers).
    // return - minutes per km
    public double calculateAveragePace(Long userId, LocalDate weekStart) {
        logger.info("Calculating average pace for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);

        double totalDistanceKm = activities.stream()
                .mapToDouble(a -> a.getDistanceM() == null ? 0 : a.getDistanceM())
                .sum() / 1000.0;

        double totalMovingMinutes = activities.stream()
                .mapToDouble(a -> a.getMovingTimeS() == null ? 0 : a.getMovingTimeS())
                .sum() / 60.0;

        if (totalDistanceKm == 0) {
            return 0;
        }
        return totalMovingMinutes / totalDistanceKm;
    }

    public double calculateAveragePaceNextWeek(Long userId, LocalDate weekStart) {
        logger.info("Calculating average pace for next week for userId: {}, weekStart: {}", userId, weekStart);
        LocalDate nextWeekStart = weekStart.plusWeeks(1);
        List<Activity> activities = getActivitiesForWeek(userId, nextWeekStart);

        double totalDistanceKm = activities.stream()
                .mapToDouble(a -> a.getDistanceM() == null ? 0 : a.getDistanceM())
                .sum() / 1000.0;

        double totalMovingMinutes = activities.stream()
                .mapToDouble(a -> a.getMovingTimeS() == null ? 0 : a.getMovingTimeS())
                .sum() / 60.0;

        if (totalDistanceKm == 0) {
            if (isEqualOrAfterThisMonday(nextWeekStart)) {
                return 0;
            } else {
                return calculateAveragePaceNextWeek(userId, nextWeekStart);
            }
        }
        return totalMovingMinutes / totalDistanceKm;
    }

    // avg heart rate per week
    public double calculateAverageHeartRate(Long userId, LocalDate weekStart) {
        logger.info("Calculating average heart rate for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        if (activities.isEmpty()) {
            return 0;
        }
        return activities.stream()
                .map(Activity::getAvgHeartrateBpm)
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0);
    }

    // weekly distance in km
    public double calculateWeeklyDistance(Long userId, LocalDate weekStart) {
        logger.info("Calculating weekly distance for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        if (activities.isEmpty()) {
            return 0;
        }
        return activities.stream()
                .mapToDouble(a -> a.getDistanceM() == null ? 0 : a.getDistanceM())
                .sum() / 1000.0;
    }

    // weekly distance in km
    public double calculateTargetDistanceNextWeek(Long userId, LocalDate weekStart) {
        logger.info("Calculating target distance for next week for userId: {}, weekStart: {}", userId, weekStart);
        LocalDate nextWeekStart = weekStart.plusWeeks(1);
        List<Activity> activities = getActivitiesForWeek(userId, nextWeekStart);
        if (activities.isEmpty()) {
            if (isEqualOrAfterThisMonday(nextWeekStart)) {
                return 0;
            } else {
                return calculateTargetDistanceNextWeek(userId, nextWeekStart);
            }
        }
        return activities.stream()
                .mapToDouble(a -> a.getDistanceM() == null ? 0 : a.getDistanceM())
                .sum() / 1000.0;
    }

    //Longest run distance in km
    public double calculateLongestRun(Long userId, LocalDate weekStart) {
        logger.info("Calculating longest run for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        if (activities.isEmpty()) {
            return 0;
        }
        return activities.stream()
                .mapToDouble(a -> a.getDistanceM() == null ? 0 : a.getDistanceM())
                .max()
                .orElse(0) / 1000.0;
    }

    //average cadence
    public double calculateAverageCadence(Long userId, LocalDate weekStart) {
        logger.info("Calculating average cadence for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        if (activities.isEmpty()) {
            return 0;
        }
        return activities.stream()
                .map(Activity::getAvgCadence)
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0) * 2;
    }

    public double calculateAllTimeAverageCadence(Long userId) {
        logger.info("Calculating all-time average cadence for userId: {}", userId);
        return nullToZero(activityRepository.getAllTimeAverageCadence(userId));
    }

    public double calculateAllTimeAverageHeartRate(Long userId) {
        logger.info("Calculating all-time average heart rate for userId: {}", userId);
        return nullToZero(activityRepository.getAllTimeAverageHeartRate(userId));
    }

    public double calculateAllTimeAverageElevationPerKm(Long userId) {
        logger.info("Calculating all-time average elevation per km for userId: {}", userId);
        return nullToZero(activityRepository.getAllTimeAverageElevationPerKm(userId));
    }

    //Training Load > Sum of (Duration in minutes×Intensity Factor)
    //Intensity Factor=Average Heart Rate/Max Heart Rate
    public double calculateTrainingLoad(Long userId, LocalDate weekStart) {
        logger.info("Calculating training load for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
       if (activities.isEmpty()) {
            return 0.0;
        }
        return activities.stream()
                .filter(activity -> activity.getAvgHeartrateBpm() != null)
                .mapToDouble(activity ->
                        (activity.getMovingTimeS() / 60.0) *
                                (activity.getAvgHeartrateBpm() / Constants.MAX_HEART_RATE)
                )
                .sum();
    }

    public double calculateWeeklyRunCount(Long userId, LocalDate weekStart) {
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        return activities.size();
    }

    // total elevation gain in meters for the week
    public double calculateWeeklyElevation(Long userId, LocalDate weekStart) {
        logger.info("Calculating elevation for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        if (activities.isEmpty()) return 0.0;
        return activities.stream()
                .mapToDouble(a -> a.getElevationGainM() == null ? 0 : a.getElevationGainM())
                .sum();
    }

    // total moving time in seconds for the week
    public long calculateTotalMovingTimeSeconds(Long userId, LocalDate weekStart) {
        logger.info("Calculating moving time for userId: {}, weekStart: {}", userId, weekStart);
        List<Activity> activities = getActivitiesForWeek(userId, weekStart);
        if (activities.isEmpty()) return 0L;
        return (long) activities.stream()
                .mapToDouble(a -> a.getMovingTimeS() == null ? 0 : a.getMovingTimeS())
                .sum();
    }

    private List<Activity> getActivitiesForWeek(Long userId, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(6)
                .atTime(23, 59, 59);

        return activityRepository.findRunningActivitiesForDateRange(
                userId,
                from,
                to
        );
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private boolean isEqualOrAfterThisMonday(LocalDate weekStart) {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return weekStart.isEqual(thisMonday) || weekStart.isAfter(thisMonday);
    }
}
