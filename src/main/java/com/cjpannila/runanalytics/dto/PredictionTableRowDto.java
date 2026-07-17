package com.cjpannila.runanalytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PredictionTableRowDto {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("week_start")
    private LocalDate weekStart;

    @JsonProperty("run_count")
    private Integer runCount;

    @JsonProperty("total_distance_km")
    private Double totalDistanceKm;

    @JsonProperty("avg_pace_min_per_km")
    private Double avgPaceMinPerKm;

    @JsonProperty("total_elevation_m")
    private Double totalElevationM;

    @JsonProperty("total_running_time_s")
    private Long totalRunningTimeS;

    @JsonProperty("avg_cadence")
    private Double avgCadence;

    @JsonProperty("avg_hr")
    private Double avgHr;

    @JsonProperty("longest_run_km")
    private Double longestRunKm;

    @JsonProperty("training_load")
    private Double trainingLoad;

    @JsonProperty("target_next_week_pace")
    private Double targetNextWeekPace;

    @JsonProperty("target_next_week_km")
    private Double targetNextWeekKm;

    @JsonProperty("target_next_week_pace_prediction")
    private Double targetNextWeekPacePrediction;

    @JsonProperty("target_next_week_km_prediction")
    private Double targetNextWeekKmPrediction;
}

