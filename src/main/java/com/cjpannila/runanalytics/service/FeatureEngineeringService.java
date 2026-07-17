package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.dto.PredictionTableRowDto;
import com.cjpannila.runanalytics.dto.TrainingDatasetExportResultDto;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.entities.WeeklySummary;
import com.cjpannila.runanalytics.repositories.ActivityRepository;
import com.cjpannila.runanalytics.repositories.UserRepository;
import com.cjpannila.runanalytics.repositories.WeeklySummaryRepository;
import com.cjpannila.runanalytics.util.Constants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureEngineeringService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureEngineeringService.class);

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final AnalyticsService analyticsService;
    private final WeeklySummaryRepository weeklySummaryRepository;

    // CSV header for the training dataset
    private static final String CSV_HEADER =
            "week_start,user_id,run_count," +
            "total_distance_km,avg_pace_min_per_km,total_elevation_m," +
            "total_running_time_s,avg_cadence,avg_hr,longest_run_km,training_load," +
            "target_next_week_pace,target_next_week_km";

    private static final String PREDICTION_CSV_HEADER =
            "user_id,week_start,run_count,total_distance_km,avg_pace_min_per_km,total_elevation_m," +
            "total_running_time_s,avg_cadence,avg_hr,longest_run_km,training_load," +
            "target_next_week_pace,target_next_week_km";

    public List<PredictionTableRowDto> buildPredictionRows() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        List<PredictionTableRowDto> rows = new ArrayList<>();

        for (User user : users) {
            Long uid = user.getUserId();
            if (uid == null) {
                continue;
            }
            List<WeeklySummary> summaries = weeklySummaryRepository.findByUser_UserIdOrderByWeekStartDesc(uid);
            if (summaries.isEmpty()) {
                continue;
            }
            int limit = Math.min(Constants.PREDICTION_DATAROWS_PER_USER, summaries.size());
            for (int i = 0; i < limit; i++) {
                WeeklySummary summary = summaries.get(i);
                rows.add(PredictionTableRowDto.builder()
                        .userId(uid)
                        .weekStart(summary.getWeekStart())
                        .runCount(summary.getRunCount())
                        .totalDistanceKm(toDouble(summary.getTotalDistanceKm()))
                        .avgPaceMinPerKm(toDouble(summary.getAvgPaceMinPerKm()))
                        .totalElevationM(toDouble(summary.getTotalElevationM()))
                        .totalRunningTimeS(summary.getTotalRunningTimeS())
                        .avgCadence(toDouble(summary.getAvgCadence()))
                        .avgHr(toDouble(summary.getAvgHr()))
                        .longestRunKm(toDouble(summary.getLongestRunKm()))
                        .trainingLoad(toDouble(summary.getTrainingLoad()))
                        .targetNextWeekPace(toDouble(summary.getTargetNextWeekPace()))
                        .targetNextWeekKm(toDouble(summary.getTargetNextWeekKm()))
                        .build());
            }
        }
        return rows;
    }

    public TrainingDatasetExportResultDto savePredictionDataset() {
        List<PredictionTableRowDto> rows = buildPredictionRows();
        TrainingDatasetExportResultDto result = new TrainingDatasetExportResultDto();
        result.setRowsGenerated(rows.size());
        result.setActivitiesUsed(rows.size());

        Path outputFile = resolvePredictionDatasetFile();
        try {
            Files.createDirectories(outputFile.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append(PREDICTION_CSV_HEADER).append("\n");
            for (PredictionTableRowDto row : rows) {
                sb.append(nullToEmpty(row.getUserId())).append(",")
                        .append(nullToEmpty(row.getWeekStart())).append(",")
                        .append(nullToEmpty(row.getRunCount())).append(",")
                        .append(nullToEmpty(row.getTotalDistanceKm())).append(",")
                        .append(nullToEmpty(row.getAvgPaceMinPerKm())).append(",")
                        .append(nullToEmpty(row.getTotalElevationM())).append(",")
                        .append(nullToEmpty(row.getTotalRunningTimeS())).append(",")
                        .append(nullToEmpty(row.getAvgCadence())).append(",")
                        .append(nullToEmpty(row.getAvgHr())).append(",")
                        .append(nullToEmpty(row.getLongestRunKm())).append(",")
                        .append(nullToEmpty(row.getTrainingLoad())).append(",")
                        .append(nullToEmpty(row.getTargetNextWeekPace())).append(",")
                        .append(nullToEmpty(row.getTargetNextWeekKm()))
                        .append("\n");
            }
            Files.writeString(outputFile, sb.toString(), java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE);
            result.setCsvBytes(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return result;
        } catch (Exception e) {
            logger.error("Failed to write prediction dataset", e);
            throw new RuntimeException("Failed to write prediction dataset", e);
        }
    }

    public TrainingDatasetExportResultDto saveWeeklySummary() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        TrainingDatasetExportResultDto result = new TrainingDatasetExportResultDto();
        result.setRowsGenerated(0);
        result.setActivitiesUsed(0);

        if (users.isEmpty()) {
            logger.warn("No users found — cannot save weekly summary");
            return result;
        }
        List<Long> allUserIds = users.stream()
                .map(User::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Find global date range across all users
        LocalDateTime minStart = activityRepository.findMinStartTimeForUsers(allUserIds);
        LocalDateTime maxStart = activityRepository.findMaxStartTimeForUsers(allUserIds);

        if (minStart == null || maxStart == null) {
            return result;
        }

        LocalDate firstMonday = minStart.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = maxStart.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        logger.info("Saving weekly summary: {} users, weeks {} → {}",
                users.size(), firstMonday, lastMonday);

        // Iterate oldest → newest week
        LocalDate weekStart = firstMonday;
        while (!weekStart.isAfter(lastMonday)) {
            for (User user : users) {
                Long uid = user.getUserId();
                if (uid == null) continue;
                Optional<WeeklySummary> existingSummary = weeklySummaryRepository.findByUserAndWeekStart(user, weekStart);
                if (existingSummary.isPresent()) {
                    continue;
                }
                double totalDistanceKm  = analyticsService.calculateWeeklyDistance(uid, weekStart);
                double avgPace = analyticsService.calculateAveragePace(uid, weekStart);
                double avgHr = analyticsService.calculateAverageHeartRate(uid, weekStart);
                double avgCadence = analyticsService.calculateAverageCadence(uid, weekStart);
                double trainingLoad = analyticsService.calculateTrainingLoad(uid, weekStart);
                double longestRunKm = analyticsService.calculateLongestRun(uid, weekStart);
                double elevation = analyticsService.calculateWeeklyElevation(uid, weekStart);
                long movingTimeS = analyticsService.calculateTotalMovingTimeSeconds(uid, weekStart);
                int runCount = (int) analyticsService.calculateWeeklyRunCount(uid, weekStart);
                double targetNextWeekPace = analyticsService.calculateAveragePaceNextWeek(uid, weekStart);
                double targetNextWeekKm = analyticsService.calculateTargetDistanceNextWeek(uid, weekStart);

                //Skip weeks with no runs for this user
                if (runCount > 0 && targetNextWeekPace > 0) {
                    if (isZeroOrNull(avgCadence)) {
                        avgCadence = analyticsService.calculateAllTimeAverageCadence(uid);
                    }
                    if (isZeroOrNull(avgHr)) {
                        avgHr = analyticsService.calculateAllTimeAverageHeartRate(uid);
                    }
                    if (isZeroOrNull(trainingLoad)) {
                        //Training Load > Sum of (Duration in minutes×Intensity Factor)
                        //Intensity Factor=Average Heart Rate/Max Heart Rate
                        trainingLoad = (movingTimeS / 60.0) * (avgHr / Constants.MAX_HEART_RATE);
                    }
                    if (isZeroOrNull(elevation)) {
                        elevation = analyticsService.calculateAllTimeAverageElevationPerKm(uid) * totalDistanceKm;
                    }
                    //No existing weekly summary present in the DB
                    WeeklySummary weeklySummary = new WeeklySummary();
                    weeklySummary.setWeekStart(weekStart);
                    weeklySummary.setUser(user);
                    weeklySummary.setRunCount(runCount);
                    weeklySummary.setTotalDistanceKm((float) round(totalDistanceKm, 2));
                    weeklySummary.setAvgPaceMinPerKm((float) round(avgPace, 2));
                    weeklySummary.setTotalElevationM((float) round(elevation, 1));
                    weeklySummary.setTotalRunningTimeS(movingTimeS);
                    weeklySummary.setAvgCadence((float) round(avgCadence, 1));
                    weeklySummary.setAvgHr((float) round(avgHr, 1));
                    weeklySummary.setLongestRunKm((float) round(longestRunKm, 2));
                    weeklySummary.setTrainingLoad((float) round(trainingLoad, 3));
                    weeklySummary.setTargetNextWeekPace((float) round(targetNextWeekPace, 2));
                    weeklySummary.setTargetNextWeekKm((float) round(targetNextWeekKm, 2));
                    // Save the weekly summary to the database
                    weeklySummaryRepository.save(weeklySummary);

                    result.setRowsGenerated(result.getRowsGenerated() + 1);
                    result.setActivitiesUsed(result.getActivitiesUsed() + runCount);
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
        logger.info("Weekly summary saved successfully");
        return result;
    }

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
                .filter(Objects::nonNull)
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

                Optional<WeeklySummary> weeklySummaryOptional =
                        weeklySummaryRepository.findByUserAndWeekStart(user, weekStart);
                if (weeklySummaryOptional.isPresent()) {
                    WeeklySummary weeklySummary = weeklySummaryOptional.get();
                    sb.append(weekStart).append(",")
                            .append(uid).append(",")
                            .append(weeklySummary.getRunCount()).append(",")
                            .append(round(weeklySummary.getTotalDistanceKm(), 2)).append(",")
                            .append(round(weeklySummary.getAvgPaceMinPerKm(), 2)).append(",")
                            .append(round(weeklySummary.getTotalElevationM(), 1)).append(",")
                            .append(weeklySummary.getTotalRunningTimeS()).append(",")
                            .append(round(weeklySummary.getAvgCadence(), 1)).append(",")
                            .append(round(weeklySummary.getAvgHr(), 1)).append(",")
                            .append(round(weeklySummary.getLongestRunKm(), 2)).append(",")
                            .append(round(weeklySummary.getTrainingLoad(), 3)).append(",")
                            .append(round(weeklySummary.getTargetNextWeekPace(), 2)).append(",")
                            .append(round(weeklySummary.getTargetNextWeekKm(), 2))
                            .append("\n");
                    result.setRowsGenerated(result.getRowsGenerated() + 1);
                    result.setActivitiesUsed(result.getActivitiesUsed() + weeklySummary.getRunCount());
                }
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

    private boolean isZeroOrNull(Number value) {
        return value == null || value.doubleValue() == 0.0;
    }

    private Path resolvePredictionDatasetFile() {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path downloadsDir = homeDir.resolve("Downloads");
        Path targetDir = Files.exists(downloadsDir) ? downloadsDir : homeDir;
        return targetDir.resolve(Constants.PREDICTION_DATASET_FILE_NAME);
    }

    private String nullToEmpty(Number value) {
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(Long value) {
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private Double toDouble(Float value) {
        return value == null ? null : value.doubleValue();
    }
}
