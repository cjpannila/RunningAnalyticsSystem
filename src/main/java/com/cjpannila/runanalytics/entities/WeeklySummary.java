package com.cjpannila.runanalytics.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WeeklySummary.WeeklySummaryId.class)
public class WeeklySummary {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @Column(name = "week_start")
    private LocalDate weekStart;

    @Column(name = "run_count")
    private Integer runCount;

    @Column(name = "total_distance_km")
    private Float totalDistanceKm;

    @Column(name = "avg_pace_min_per_km")
    private Float avgPaceMinPerKm;

    @Column(name = "total_elevation_m")
    private Float totalElevationM;

    @Column(name = "total_running_time_s")
    private Long totalRunningTimeS;

    @Column(name = "avg_cadence")
    private Float avgCadence;

    @Column(name = "avg_hr")
    private Float avgHr;

    @Column(name = "longest_run_km")
    private Float longestRunKm;

    @Column(name = "training_load")
    private Float trainingLoad;

    @Column(name = "target_next_week_pace")
    private Float targetNextWeekPace;

    @Column(name = "target_next_week_km")
    private Float targetNextWeekKm;

    @Column(name = "predicted_next_week_pace")
    private Float predictedNextWeekPace;

    @Column(name = "predicted_next_week_km")
    private Float predictedNextWeekKm;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklySummaryId implements Serializable {
        private Long user;
        private LocalDate weekStart;
    }
}