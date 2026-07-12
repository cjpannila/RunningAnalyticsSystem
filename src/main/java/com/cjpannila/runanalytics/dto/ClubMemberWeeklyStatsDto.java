package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMemberWeeklyStatsDto {
    private Long userId;
    private String runnerName;
    private Integer runCount;
    private Double totalDistanceKm;
    private Double averagePaceMinPerKm;
    private Double totalElevationM;
    private Long totalRunningTimeS;
}

