package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.repositories.ActivityRepository;
import com.cjpannila.runanalytics.service.UserService;
import com.cjpannila.runanalytics.service.AnalyticsService;
import com.cjpannila.runanalytics.dto.UserWeeklyStatsDto;
import com.cjpannila.runanalytics.dto.StravaTokenRequest;
import com.cjpannila.runanalytics.dto.StravaTokenResponse;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.repositories.UserRepository;
import com.cjpannila.runanalytics.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {
    Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final ActivityRepository activityRepository;
    private final RestTemplate restTemplate;

    @Value("${strava.client.id}")
    private String stravaClientId;

    @Value("${strava.client.secret}")
    private String stravaClientSecret;

    @Value("${strava.oauth.token.url}")
    private String stravaTokenUrl;

    public UserController(UserService userService, UserRepository userRepository, AnalyticsService analyticsService, ActivityRepository activityRepository, RestTemplate restTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
        this.activityRepository = activityRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping(value = "/users/weekly-stats")
    public Object getUsersWeeklyStats(@RequestParam(required = false) String weekStart,
                                      @RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) String userIds) {
        try {
            boolean allWeeks = weekStart != null && "ALL".equalsIgnoreCase(weekStart.trim());
            LocalDate selectedWeekStart = null;
            LocalDate weekEnd = null;
            if (!allWeeks) {
                if (weekStart == null || weekStart.isBlank()) {
                    selectedWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                } else {
                    selectedWeekStart = LocalDate.parse(weekStart).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                }
                weekEnd = selectedWeekStart.plusDays(6);
            }

            List<User> users = userService.getUsers();

            // parse optional CSV of userIds (higher priority than single userId)
            final Set<Long> idFilter = new HashSet<>();
            if (userIds != null && !userIds.isBlank()) {
                for (String s : userIds.split(",")) {
                        try {
                            idFilter.add(Long.parseLong(s.trim()));
                        } catch (NumberFormatException nfe) {
                            // Skip invalid id values in the CSV. Log at warn so we can inspect bad input if needed.
                            logger.warn("Invalid userId '{}' in userIds CSV - skipping", s);
                        }
                }
            }

            Stream<User> stream = users.stream();
            if (!idFilter.isEmpty()) {
                stream = stream.filter(u -> u.getUserId() != null && idFilter.contains(u.getUserId()));
            } else if (userId != null) {
                stream = stream.filter(u -> u.getUserId() != null && u.getUserId().equals(userId));
            }
            if (search != null && !search.isBlank()) {
                String lower = search.toLowerCase();
                stream = stream.filter(u -> ((u.getFirstname() != null ? u.getFirstname() : "") + " " + (u.getLastname() != null ? u.getLastname() : "")).toLowerCase().contains(lower));
            }

            List<UserWeeklyStatsDto> results = new ArrayList<>();

            // collect the filtered users once (we will iterate per-week and per-user)
            List<User> filteredUsers = stream.collect(Collectors.toList());

            // if client requested ALL weeks, return a row per user per week from first->last
            if ("ALL".equalsIgnoreCase(weekStart != null ? weekStart : "")) {
                // build id list for range queries
                List<Long> idsForRange = new ArrayList<>();
                if (!idFilter.isEmpty()) idsForRange.addAll(idFilter);
                else if (userId != null) idsForRange.add(userId);
                if (idsForRange.isEmpty()) filteredUsers.forEach(x -> { if (x.getUserId() != null) idsForRange.add(x.getUserId()); });

                LocalDateTime minStart = idsForRange.isEmpty() ? null : activityRepository.findMinStartTimeForUsers(idsForRange);
                LocalDateTime maxStart = idsForRange.isEmpty() ? null : activityRepository.findMaxStartTimeForUsers(idsForRange);
                if (minStart == null || maxStart == null) {
                    // no activity range -> return empty users list
                } else {
                    LocalDate firstMonday = minStart.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate lastMonday = maxStart.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                    // iterate weeks newest->oldest
                    LocalDate cur = lastMonday;
                    while (!cur.isBefore(firstMonday)) {
                        final LocalDate weekStartDate = cur;
                        // for each user, compute weekly metrics using AnalyticsService
                        for (User u : filteredUsers) {
                            Long uid = u.getUserId();
                            double totalDistance = analyticsService.calculateWeeklyDistance(uid, weekStartDate);
                            double avgPace = analyticsService.calculateAveragePace(uid, weekStartDate);
                            double avgHr = analyticsService.calculateAverageHeartRate(uid, weekStartDate);
                            double avgCadence = analyticsService.calculateAverageCadence(uid, weekStartDate);
                            double trainingLoad = analyticsService.calculateTrainingLoad(uid, weekStartDate);
                            double longest = analyticsService.calculateLongestRun(uid, weekStartDate);
                            double elevation = analyticsService.calculateWeeklyElevation(uid, weekStartDate);
                            long movingSeconds = analyticsService.calculateTotalMovingTimeSeconds(uid, weekStartDate);
                            double runCount = analyticsService.calculateWeeklyRunCount(uid, weekStartDate);

                            UserWeeklyStatsDto dto = UserWeeklyStatsDto.builder()
                                    .userId(uid)
                                    .firstname(u.getFirstname())
                                    .lastname(u.getLastname())
                                    .weekStart(weekStartDate.toString())
                                    .runCount((int) runCount)
                                    .totalDistanceKm(round(totalDistance, 2))
                                    .averagePaceMinPerKm(Double.isNaN(avgPace)?null:round(avgPace, 2))
                                    .averageHeartRate(Double.isNaN(avgHr)?null:round(avgHr, 1))
                                    .averageCadence(Double.isNaN(avgCadence)?null:round(avgCadence, 0))
                                    .trainingLoad(Double.isNaN(trainingLoad)?null:round(trainingLoad, 2))
                                    .longestRunKm(round(longest, 2))
                                    .totalElevationM(round(elevation, 1))
                                    .totalRunningTimeS(movingSeconds)
                                    .build();
                            results.add(dto);
                        }
                        cur = cur.minusWeeks(1);
                    }
                }
            } else {
                // single selected week: compute metrics per filtered user for that week
                final LocalDate weekStartForLambda = selectedWeekStart;
                for (User u : filteredUsers) {
                    Long uid = u.getUserId();
                    double totalDistance = analyticsService.calculateWeeklyDistance(uid, weekStartForLambda);
                    double avgPace = analyticsService.calculateAveragePace(uid, weekStartForLambda);
                    double avgHr = analyticsService.calculateAverageHeartRate(uid, weekStartForLambda);
                    double avgCadence = analyticsService.calculateAverageCadence(uid, weekStartForLambda);
                    double trainingLoad = analyticsService.calculateTrainingLoad(uid, weekStartForLambda);
                    double longest = analyticsService.calculateLongestRun(uid, weekStartForLambda);
                    double elevation = analyticsService.calculateWeeklyElevation(uid, weekStartForLambda);
                    long movingSeconds = analyticsService.calculateTotalMovingTimeSeconds(uid, weekStartForLambda);
                    double runCount = analyticsService.calculateWeeklyRunCount(uid, weekStartForLambda);

                    UserWeeklyStatsDto dto = UserWeeklyStatsDto.builder()
                            .userId(uid)
                            .firstname(u.getFirstname())
                            .lastname(u.getLastname())
                            .weekStart(weekStartForLambda != null ? weekStartForLambda.toString() : null)
                            .runCount((int) runCount)
                            .totalDistanceKm(round(totalDistance, 2))
                            .averagePaceMinPerKm(round(avgPace, 2))
                            .averageHeartRate(round(avgHr, 1))
                            .averageCadence(round(avgCadence, 0))
                            .trainingLoad(round(trainingLoad, 2))
                            .longestRunKm(round(longest, 2))
                            .totalElevationM(round(elevation, 1))
                            .totalRunningTimeS(movingSeconds)
                            .build();
                    results.add(dto);
                }
            }

            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("weekStart", (selectedWeekStart != null) ? selectedWeekStart.toString() : "ALL");
            resp.put("weekEnd", (weekEnd != null) ? weekEnd.toString() : "ALL");
            resp.put("users", results);
            return resp;
        } catch (Exception e) {
            logger.error("Error computing weekly stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Failed to compute weekly stats"));
        }
    }

    private double round(double value, int precision) {
        double factor = Math.pow(10, precision);
        return Math.round(value * factor) / factor;
    }

    @GetMapping(value = "/apiinfo")
    public Map<String, String> apiInfo() {
        logger.info("Request in: /apiinfo");
        Map<String, String> resultsMap = new HashMap<>();
        resultsMap.put("Application", Constants.APPLICATION_NAME);
        resultsMap.put("Version", Constants.APPLICATION_VERSION);
        return resultsMap;
    }

    @GetMapping(value = "/users")
    public List<User> users() {
        logger.info("Getting all users");
        return userService.getUsers();
    }

    @GetMapping(value = "/user")
    public User getUser(@RequestParam String firstName, @RequestParam String lastName) throws Exception {
        return userService.loadUserByUsername(firstName, lastName);
    }

    @PostMapping(value = "/authenticate")
    public ResponseEntity<?> authenticate(@RequestParam String code) {
        try {
            logger.info("Authenticating with Strava code via API");

            // Create token request
            StravaTokenRequest tokenRequest = StravaTokenRequest.builder()
                    .client_id(stravaClientId)
                    .client_secret(stravaClientSecret)
                    .code(code)
                    .grant_type(Constants.GRANT_TYPE)
                    .build();

            // Call Strava token endpoint
            StravaTokenResponse tokenResponse = restTemplate.postForObject(
                    stravaTokenUrl,
                    tokenRequest,
                    StravaTokenResponse.class
            );

            if (tokenResponse == null || tokenResponse.getAthlete() == null) {
                logger.error("Invalid response from Strava");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid response from Strava"));
            }

            // Create or update user
            Long athleteId = tokenResponse.getAthlete().getId();
            User user = userRepository.findById(athleteId)
                    .orElseGet(() -> User.builder()
                            .userId(athleteId)
                            .firstname(tokenResponse.getAthlete().getFirstname())
                            .lastname(tokenResponse.getAthlete().getLastname())
                            .city(tokenResponse.getAthlete().getCity())
                            .country(tokenResponse.getAthlete().getCountry())
                            .sex(tokenResponse.getAthlete().getSex())
                            .build()
                    );

            // Update token and profile details
            user.setAccessToken(tokenResponse.getAccessToken());
            user.setRefreshToken(tokenResponse.getRefreshToken());
            user.setTokenExpiresAt(tokenResponse.getExpiresAt());
            user.setCity(tokenResponse.getAthlete().getCity());
            user.setCountry(tokenResponse.getAthlete().getCountry());
            user.setSex(tokenResponse.getAthlete().getSex());

            // Save user to database
            User savedUser = userRepository.save(user);
            logger.info("User saved successfully: {}", savedUser.getUserId());

            return ResponseEntity.ok(Map.of(
                    "message", "User authenticated and saved successfully",
                    "userId", savedUser.getUserId(),
                    "firstname", savedUser.getFirstname(),
                    "lastname", savedUser.getLastname(),
                    "city", savedUser.getCity(),
                    "country", savedUser.getCountry()
            ));

        } catch (Exception e) {
            logger.error("Error during Strava authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping(value = "/page/authenticate")
    public RedirectView authenticateViaPage(@RequestParam String code) {
        try {
            logger.info("Authenticating with Strava code via User Interface");

            // Create token request
            StravaTokenRequest tokenRequest = StravaTokenRequest.builder()
                    .client_id(stravaClientId)
                    .client_secret(stravaClientSecret)
                    .code(code)
                    .grant_type(Constants.GRANT_TYPE)
                    .build();

            // Call Strava token endpoint
            StravaTokenResponse tokenResponse = restTemplate.postForObject(
                    stravaTokenUrl,
                    tokenRequest,
                    StravaTokenResponse.class
            );

            if (tokenResponse == null || tokenResponse.getAthlete() == null) {
                logger.error("Invalid response from Strava");
                return authenticatedErrorRedirect("Invalid response from Strava");
            }

            // Create or update user
            Long athleteId = tokenResponse.getAthlete().getId();
            User user = userRepository.findById(athleteId)
                    .orElseGet(() -> User.builder()
                            .userId(athleteId)
                            .firstname(tokenResponse.getAthlete().getFirstname())
                            .lastname(tokenResponse.getAthlete().getLastname())
                            .city(tokenResponse.getAthlete().getCity())
                            .country(tokenResponse.getAthlete().getCountry())
                            .sex(tokenResponse.getAthlete().getSex())
                            .build()
                    );

            // Update token and profile details
            user.setAccessToken(tokenResponse.getAccessToken());
            user.setRefreshToken(tokenResponse.getRefreshToken());
            user.setTokenExpiresAt(tokenResponse.getExpiresAt());
            user.setCity(tokenResponse.getAthlete().getCity());
            user.setCountry(tokenResponse.getAthlete().getCountry());
            user.setSex(tokenResponse.getAthlete().getSex());

            // Save user to database
            User savedUser = userRepository.save(user);
            logger.info("User saved successfully: {}", savedUser.getUserId());

            // Redirect to success page with userId
            return new RedirectView("/runanalytics/authenticated.html?userId=" + savedUser.getUserId());

        } catch (Exception e) {
            logger.error("Error during Strava authentication", e);
            return authenticatedErrorRedirect("Authentication failed: " + e.getMessage());
        }
    }

    private RedirectView authenticatedErrorRedirect(String errorMessage) {
        String redirectUrl = UriComponentsBuilder
                .fromPath("/runanalytics/authenticated.html")
                .queryParam("error", errorMessage)
                .build()
                .encode()
                .toUriString();
        return new RedirectView(redirectUrl);
    }

    @PostMapping(value = "/gettoken")
    public ResponseEntity<?> getAccessToken(@RequestParam String userId) {
        try {
            String accessToken = userService.getAccessToken(userId);
            if  (accessToken != null && !accessToken.isEmpty()) {
                // Include expires_at (epoch seconds) from the saved user record
                try {
                    Long uid = Long.parseLong(userId);
                    Optional<User> userOpt = userRepository.findById(uid);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        Long expiresAt = user.getTokenExpiresAt();
                        return ResponseEntity.ok(Map.of(
                                "access_token", accessToken,
                                "expires_at", expiresAt != null ? expiresAt : 0L
                        ));
                    }
                } catch (NumberFormatException nfe) {
                    // invalid userId format — fall back to returning access token only
                    logger.warn("Invalid userId format when returning expires_at: {}", userId);
                }
                return ResponseEntity.ok(Map.of("access_token", accessToken));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Access token not found for user"));
            }
        } catch (NumberFormatException nfe) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid userId"));
        } catch (Exception e) {
            logger.error("Error refreshing access token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve or refresh access token"));
        }
    }
}
