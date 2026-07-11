package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.dto.ActivityDto;
import com.cjpannila.runanalytics.entities.Activity;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.repositories.ActivityRepository;
import com.cjpannila.runanalytics.repositories.UserRepository;
import com.cjpannila.runanalytics.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {
    Logger logger = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;

    @Value("${strava.api.url}")
    private String stravaApiUrl;

    public ActivityService(ActivityRepository activityRepository,
                           UserRepository userRepository,
                           UserService userService,
                           RestTemplate restTemplate) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.restTemplate = restTemplate;
    }

    public List<Activity> getActivitiesForUser(Long userId) {
        return new ArrayList<>(activityRepository.findByUser_UserId(userId));
    }

    public List<Activity> fetchAndSaveActivities(Long userId) throws Exception {
        String accessToken = userService.getAccessToken(String.valueOf(userId));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<ActivityDto> stravaActivities = new ArrayList<>();
        int perPage = 200;
        Long after = getAfterTimestampForUser(userId);

        boolean hasMore = true;
        int page = 1;
        while (hasMore) {
            StringBuilder url = new StringBuilder(stravaApiUrl)
                    .append(Constants.API_ATHLETE_ACTIVITIES)
                    .append("?page=").append(page)
                    .append("&per_page=").append(perPage);

            if (after != null) {
                url.append("&after=").append(after);
            }
            logger.info("Fetching activities for user {} from Strava API: {}", userId, url);

            ResponseEntity<ActivityDto[]> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    ActivityDto[].class
            );

            ActivityDto[] responseBody = response.getBody();
            if (responseBody == null || responseBody.length == 0) {
                hasMore = false;
            } else {
                stravaActivities.addAll(Arrays.asList(responseBody));
                hasMore = responseBody.length == perPage;
                page++;
            }
        }

        if (stravaActivities.isEmpty()) {
            logger.info("No new activities found for user {}", userId);
            return new ArrayList<>();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Activity> savedActivities = new ArrayList<>();
        for (ActivityDto stravaActivity : stravaActivities) {
            Optional<Activity> existing = activityRepository.findById(stravaActivity.getId());
            if (existing.isPresent()) {
                continue;
            }
            Activity activity = new Activity();
            activity.setActivityId(stravaActivity.getId());
            activity.setUser(user);
            activity.setActivityName(stravaActivity.getName());
            activity.setActivityType(stravaActivity.getType() != null ? stravaActivity.getType() : stravaActivity.getSportType());
            activity.setStartTime(parseDateTime(stravaActivity.getStartDateLocal(), stravaActivity.getStartDate()));
            activity.setDistanceM(stravaActivity.getDistance());
            activity.setMovingTimeS(stravaActivity.getMovingTime());
            activity.setElapsedTimeS(stravaActivity.getElapsedTime());
            activity.setElevationGainM(stravaActivity.getTotalElevationGain());
            activity.setAvgSpeedMps(stravaActivity.getAverageSpeed());
            activity.setMaxSpeedMps(stravaActivity.getMaxSpeed());
            activity.setAvgCadence(stravaActivity.getAverageCadence());
            activity.setAvgHeartrateBpm(stravaActivity.getAverageHeartrate());
            activity.setMaxHeartrateBpm(stravaActivity.getMaxHeartrate());
            activity.setDeviceName(stravaActivity.getDeviceName());
            activity.setGearId(stravaActivity.getGearId());
            savedActivities.add(activityRepository.save(activity));
        }
        logger.info("Fetched and saved {} activities for user {}", savedActivities.size(), userId);
        return savedActivities;
    }

    private Long getAfterTimestampForUser(Long userId) {
        return activityRepository.findTopByUser_UserIdOrderByStartTimeDesc(userId)
                .map(Activity::getStartTime)
                .map(startTime -> startTime.toEpochSecond(ZoneOffset.UTC))
                .orElse(null);
    }

    private LocalDateTime parseDateTime(String startDateLocal, String startDate) {
        String dateTime = startDateLocal != null ? startDateLocal : startDate;
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTime).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(dateTime.replace("Z", ""));
            } catch (Exception e) {
                logger.warn("Unable to parse activity timestamp: {}", dateTime);
                return null;
            }
        }
    }
}
