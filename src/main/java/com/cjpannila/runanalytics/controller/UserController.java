package com.cjpannila.runanalytics.controller;

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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class UserController {
    Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final RestTemplate restTemplate;

    @Value("${strava.client.id}")
    private String stravaClientId;

    @Value("${strava.client.secret}")
    private String stravaClientSecret;

    @Value("${strava.oauth.token.url}")
    private String stravaTokenUrl;

    public UserController(UserService userService, UserRepository userRepository, AnalyticsService analyticsService, RestTemplate restTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
        this.restTemplate = restTemplate;
    }

    @GetMapping(value = "/users/weekly-stats")
    public Object getUsersWeeklyStats(@RequestParam(required = false) String weekStart,
                                      @RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) String userIds) {
        try {
            LocalDate selectedWeekStart;
            if (weekStart == null || weekStart.isBlank()) {
                selectedWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            } else {
                selectedWeekStart = LocalDate.parse(weekStart).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            }

            LocalDate weekEnd = selectedWeekStart.plusDays(6);

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
            stream.forEach(u -> {
                Long uid = u.getUserId();
                double totalDistance = analyticsService.calculateWeeklyDistance(uid, selectedWeekStart);
                double avgPace = analyticsService.calculateAveragePace(uid, selectedWeekStart);
                double avgHr = analyticsService.calculateAverageHeartRate(uid, selectedWeekStart);
                double avgCadence = analyticsService.calculateAverageCadence(uid, selectedWeekStart);
                double trainingLoad = analyticsService.calculateTrainingLoad(uid, selectedWeekStart);
                double longest = analyticsService.calculateLongestRun(uid, selectedWeekStart);
                double elevation = analyticsService.calculateWeeklyElevation(uid, selectedWeekStart);
                long movingSeconds = analyticsService.calculateTotalMovingTimeSeconds(uid, selectedWeekStart);
                double runCount = analyticsService.calculateWeeklyRunCount(uid, selectedWeekStart);

                UserWeeklyStatsDto dto = UserWeeklyStatsDto.builder()
                        .userId(uid)
                        .firstname(u.getFirstname())
                        .lastname(u.getLastname())
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
            });

            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("weekStart", selectedWeekStart.toString());
            resp.put("weekEnd", weekEnd.toString());
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
                return new RedirectView("/runanalytics/authenticated.html?error=Invalid response from Strava");
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
            return new RedirectView("/runanalytics/authenticated.html?error=Authentication failed: " + e.getMessage());
        }
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
