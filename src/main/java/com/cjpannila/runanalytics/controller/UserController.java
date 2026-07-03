package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.UserService;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/api")
public class UserController {
    Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${strava.client.id}")
    private String stravaClientId;

    @Value("${strava.client.secret}")
    private String stravaClientSecret;

    @Value("${strava.oauth.token.url}")
    private String stravaTokenUrl;

    public UserController(UserService userService, UserRepository userRepository, RestTemplate restTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
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
        return userService.getUsers();
    }

    @GetMapping(value = "/user")
    public User getUser() throws Exception {
        return userService.loadUserByUsername("Chinthana", "Pannila");
    }

    @PostMapping(value = "/authenticate")
    public ResponseEntity<?> authenticate(@RequestParam String code) {
        try {
            logger.info("Authenticating with Strava code");

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
            logger.info("User saved successfully: {} {}", savedUser.getFirstname(), savedUser.getLastname());

            return ResponseEntity.ok(Map.of(
                    "message", "User authenticated and saved successfully",
                    "userId", savedUser.getUserId(),
                    "firstname", savedUser.getFirstname(),
                    "lastname", savedUser.getLastname()
            ));

        } catch (Exception e) {
            logger.error("Error during Strava authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/gettoken")
    public ResponseEntity<?> getAccessToken(@RequestParam String userId) {
        try {
            String accessToken = userService.getAccessToken(userId);
            if  (accessToken != null && !accessToken.isEmpty()) {
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
