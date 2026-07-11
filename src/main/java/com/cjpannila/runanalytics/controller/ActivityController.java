package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.entities.Activity;
import com.cjpannila.runanalytics.service.ActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/activities/sync")
    public ResponseEntity<Object> fetchAndSaveActivities(@RequestParam Long userId) {
        logger.info("Fetching activities for user: {}", userId);
        try {
            List<Activity> savedActivities = activityService.fetchAndSaveActivities(userId);
            logger.info("Successfully fetched and saved activities for user: {}", userId);
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
        logger.info("Getting saved activities for user: {}", userId);
        try {
            return ResponseEntity.ok(activityService.getActivitiesForUser(userId));
        } catch (Exception e) {
            logger.error("Error loading activities for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load activities: " + e.getMessage()));
        }
    }
}
