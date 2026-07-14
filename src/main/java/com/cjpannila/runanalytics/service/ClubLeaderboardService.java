package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.dto.ClubMemberWeeklyStatsDto;
import com.cjpannila.runanalytics.dto.ClubWeeklyStatsResponseDto;
import com.cjpannila.runanalytics.entities.Activity;
import com.cjpannila.runanalytics.entities.Club;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.entities.UserClub;
import com.cjpannila.runanalytics.repositories.ActivityRepository;
import com.cjpannila.runanalytics.repositories.ClubRepository;
import com.cjpannila.runanalytics.repositories.UserClubRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClubLeaderboardService {

    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final UserClubRepository userClubRepository;

    public ClubLeaderboardService(ActivityRepository activityRepository,
                                  ClubRepository clubRepository,
                                  UserClubRepository userClubRepository) {
        this.activityRepository = activityRepository;
        this.clubRepository = clubRepository;
        this.userClubRepository = userClubRepository;
    }

    public ClubWeeklyStatsResponseDto getWeeklyStats(Long clubId, LocalDate weekStart) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndExclusive = weekStartDateTime.plusDays(7);

        List<UserClub> userClubs = userClubRepository.findById_ClubId(clubId);
        Map<Long, AggregateStats> statsByUser = new LinkedHashMap<>();

        for (UserClub userClub : userClubs) {
            User user = userClub.getUser();
            if (user == null || user.getUserId() == null) {
                continue;
            }

            Long userId = user.getUserId();
            statsByUser.put(userId, new AggregateStats(userId, buildRunnerName(user)));
        }

        List<Activity> runActivities = activityRepository.findClubRunActivitiesForWeek(
                clubId,
                weekStartDateTime,
                weekEndExclusive
        );

        for (Activity activity : runActivities) {
            if (activity.getUser() != null && activity.getUser().getUserId() != null) {
                AggregateStats aggregate = statsByUser.get(activity.getUser().getUserId());
                if (aggregate != null) {
                    aggregate.runCount += 1;
                    aggregate.totalDistanceM += safeFloat(activity.getDistanceM());
                    aggregate.totalElevationM += safeFloat(activity.getElevationGainM());
                    aggregate.totalRunningTimeS += safeInt(activity.getMovingTimeS());
                    aggregate.longestRunM = Math.max(aggregate.longestRunM, safeFloat(activity.getDistanceM()));
                }
            }
        }

        List<ClubMemberWeeklyStatsDto> memberStats = new ArrayList<>();
        for (AggregateStats aggregate : statsByUser.values()) {
            memberStats.add(ClubMemberWeeklyStatsDto.builder()
                    .userId(aggregate.userId)
                    .runnerName(aggregate.runnerName)
                    .runCount(aggregate.runCount)
                    .totalDistanceKm(round(aggregate.totalDistanceM / 1000.0, 2))
                    .averagePaceMinPerKm(calculatePace(aggregate.totalDistanceM, aggregate.totalRunningTimeS))
                    .totalElevationM(round(aggregate.totalElevationM, 1))
                    .totalRunningTimeS(aggregate.totalRunningTimeS)
                    .longestRunKm(round(aggregate.longestRunM / 1000.0, 2))
                    .build());
        }

        return ClubWeeklyStatsResponseDto.builder()
                .clubId(clubId)
                .clubName(club.getName())
                .weekStart(weekStart)
                .weekEnd(weekStart.plusDays(6))
                .members(memberStats)
                .build();
    }

    private double round(double value, int precision) {
        double factor = Math.pow(10, precision);
        return Math.round(value * factor) / factor;
    }

    private Double calculatePace(double distanceM, long movingTimeS) {
        if (distanceM <= 0 || movingTimeS <= 0) {
            return null;
        }
        double minutes = movingTimeS / 60.0;
        double kilometers = distanceM / 1000.0;
        return round(minutes / kilometers, 2);
    }

    private String buildRunnerName(User user) {
        String firstname = user.getFirstname() != null ? user.getFirstname().trim() : "";
        String lastname = user.getLastname() != null ? user.getLastname().trim() : "";
        String fullName = (firstname + " " + lastname).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return "Runner " + user.getUserId();
    }

    private float safeFloat(Float value) {
        return value != null ? value : 0f;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static class AggregateStats {
        private final Long userId;
        private final String runnerName;
        private int runCount;
        private double totalDistanceM;
        private double totalElevationM;
        private long totalRunningTimeS;
        private double longestRunM;

        private AggregateStats(Long userId, String runnerName) {
            this.userId = userId;
            this.runnerName = runnerName;
            this.runCount = 0;
            this.totalDistanceM = 0;
            this.totalElevationM = 0;
            this.totalRunningTimeS = 0;
            this.longestRunM = 0;
        }
    }
}

