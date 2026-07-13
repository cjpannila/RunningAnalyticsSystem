package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWeeklyStatsDto {
    private Long userId;
    private String firstname;
    private String lastname;
    private Integer runCount;
    private Double totalDistanceKm;
    private Double averagePaceMinPerKm;
    private Double totalElevationM;
    private Long totalRunningTimeS;
    private Double averageHeartRate;
    private Double longestRunKm;
    private Double trainingLoad;
}

