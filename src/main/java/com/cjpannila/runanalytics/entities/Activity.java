package com.cjpannila.runanalytics.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(name = "activity_name")
    private String activityName;

    @Column(name = "activity_type", length = 50)
    private String activityType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "distance_m")
    private Float distanceM;

    @Column(name = "moving_time_s")
    private Integer movingTimeS;

    @Column(name = "elapsed_time_s")
    private Integer elapsedTimeS;

    @Column(name = "elevation_gain_m")
    private Float elevationGainM;

    @Column(name = "avg_speed_mps")
    private Float avgSpeedMps;

    @Column(name = "max_speed_mps")
    private Float maxSpeedMps;

    @Column(name = "avg_cadence")
    private Float avgCadence;

    @Column(name = "avg_heartrate_bpm")
    private Float avgHeartrateBpm;

    @Column(name = "max_heartrate_bpm")
    private Float maxHeartrateBpm;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "gear_id", length = 50)
    private String gearId;
}

