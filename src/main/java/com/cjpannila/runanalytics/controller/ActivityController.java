package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.entities.Activity;
import com.cjpannila.runanalytics.service.ActivityService;
import com.cjpannila.runanalytics.service.ClubService;
import com.cjpannila.runanalytics.util.ResponseTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ActivityController {
    private final Logger logger = LoggerFactory.getLogger(ActivityController.class);

    private final ActivityService activityService;
    private final ClubService clubService;

    public ActivityController(ActivityService activityService, ClubService clubService) {
        this.activityService = activityService;
        this.clubService = clubService;
    }

    /**
     * Fetch all activities from Strava API for the given user and save them to the database.
     * Also save clubs for user
     * @param userId
     * @return
     */
    @PostMapping("/activities/sync")
    public ResponseEntity<Object> fetchAndSaveActivities(@RequestParam Long userId) {
        StopWatch watch = ResponseTimeUtil.getStopWatchAndStart();
        logger.info("Fetching activities for user: {}", userId);
        try {
            List<Activity> savedActivities = activityService.fetchAndSaveActivities(userId);
            //Save clubs for user too at the same time
            clubService.callClubsApiAndSavetoDB(userId);
            logger.info("Successfully fetched and saved activities for user: {}", userId);
            ResponseTimeUtil.stopAndLogResponseTime(logger, "/api/activities/sync", watch);
            return ResponseEntity.ok(Map.of(
                    "message", "Activities fetched and saved successfully",
                    "userId", userId,
                    "savedCount", savedActivities.size(),
                    "activities", savedActivities
            ));
        } catch (Exception e) {
            logger.error("Error fetching activities for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch activities: " + e.getMessage()));
        }
    }

    @GetMapping("/activities/saved")
    public ResponseEntity<Object> getActivities(@RequestParam Long userId) {
        StopWatch watch = ResponseTimeUtil.getStopWatchAndStart();
        logger.info("Getting saved activities for user: {}", userId);
        try {
            List<Activity> savedActivities = activityService.getActivitiesForUser(userId);
            ResponseTimeUtil.stopAndLogResponseTime(logger, "/api/activities/saved", watch);
            return ResponseEntity.ok(savedActivities);
        } catch (Exception e) {
            logger.error("Error loading activities for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load activities: " + e.getMessage()));
        }
    }
}
