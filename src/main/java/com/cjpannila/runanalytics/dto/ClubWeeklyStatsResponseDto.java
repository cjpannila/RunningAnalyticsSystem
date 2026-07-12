package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubWeeklyStatsResponseDto {
    private Long clubId;
    private String clubName;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<ClubMemberWeeklyStatsDto> members;
}

