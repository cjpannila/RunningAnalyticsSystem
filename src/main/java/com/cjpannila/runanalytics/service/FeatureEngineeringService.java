package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.dto.TrainingDatasetExportResultDto;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.repositories.ActivityRepository;
import com.cjpannila.runanalytics.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureEngineeringService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureEngineeringService.class);

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final AnalyticsService analyticsService;

    // CSV header for the training dataset
    private static final String CSV_HEADER =
            "week_start,user_id,run_count," +
            "total_distance_km,avg_pace_min_per_km,total_elevation_m," +
            "total_running_time_s,avg_cadence,avg_hr,longest_run_km,training_load";

    //Generates a training dataset CSV covering ALL users across ALL weeks
    public TrainingDatasetExportResultDto generateTrainingDatasetCsv() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        TrainingDatasetExportResultDto result = new TrainingDatasetExportResultDto();
        result.setRowsGenerated(0);
        result.setActivitiesUsed(0);

        if (users.isEmpty()) {
            logger.warn("No users found — returning header-only CSV");
            result.setCsvBytes((CSV_HEADER + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return result;
        }

        List<Long> allUserIds = users.stream()
                .map(User::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        // Find global date range across all users
        LocalDateTime minStart = activityRepository.findMinStartTimeForUsers(allUserIds);
        LocalDateTime maxStart = activityRepository.findMaxStartTimeForUsers(allUserIds);

        if (minStart == null || maxStart == null) {
            logger.warn("No run activities found — returning header-only CSV");
            result.setCsvBytes((CSV_HEADER + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return result;
        }

        LocalDate firstMonday = minStart.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = maxStart.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        logger.info("Generating training dataset: {} users, weeks {} → {}",
                users.size(), firstMonday, lastMonday);

        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");

        // Iterate oldest → newest week
        LocalDate weekStart = firstMonday;
        while (!weekStart.isAfter(lastMonday)) {
            for (User user : users) {
                Long uid = user.getUserId();
                if (uid == null) continue;

                double totalDistanceKm  = analyticsService.calculateWeeklyDistance(uid, weekStart);
                double avgPace = analyticsService.calculateAveragePace(uid, weekStart);
                double avgHr = analyticsService.calculateAverageHeartRate(uid, weekStart);
                double avgCadence = analyticsService.calculateAverageCadence(uid, weekStart);
                double trainingLoad = analyticsService.calculateTrainingLoad(uid, weekStart);
                double longestRunKm = analyticsService.calculateLongestRun(uid, weekStart);
                double elevation = analyticsService.calculateWeeklyElevation(uid, weekStart);
                long movingTimeS = analyticsService.calculateTotalMovingTimeSeconds(uid, weekStart);
                int runCount = (int) analyticsService.calculateWeeklyRunCount(uid, weekStart);

                sb.append(weekStart).append(",")
                  .append(uid).append(",")
                  .append(runCount).append(",")
                  .append(round(totalDistanceKm, 2)).append(",")
                  .append(round(avgPace, 2)).append(",")
                  .append(round(elevation, 1)).append(",")
                  .append(movingTimeS).append(",")
                  .append(round(avgCadence, 1)).append(",")
                  .append(round(avgHr, 1)).append(",")
                  .append(round(longestRunKm, 2)).append(",")
                  .append(round(trainingLoad, 3))
                  .append("\n");
                result.setRowsGenerated(result.getRowsGenerated() + 1);
                result.setActivitiesUsed(result.getActivitiesUsed() + runCount);
            }
            weekStart = weekStart.plusWeeks(1);
        }
        logger.info("Training dataset generation complete");
        result.setCsvBytes(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return result;
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /** Wraps a CSV field in quotes if it contains a comma, quote or newline */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
