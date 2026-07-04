package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.dto.ClubDto;
import com.cjpannila.runanalytics.dto.StravaClub;
import com.cjpannila.runanalytics.entities.Club;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.entities.UserClub;
import com.cjpannila.runanalytics.repositories.ClubRepository;
import com.cjpannila.runanalytics.repositories.UserClubRepository;
import com.cjpannila.runanalytics.UserService;
import com.cjpannila.runanalytics.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static com.cjpannila.runanalytics.util.Constants.API_ATHLETE_CLUBS;

@RestController
@RequestMapping("/api")
public class ClubsController {

    Logger logger = LoggerFactory.getLogger(ClubsController.class);

    private final UserService userService;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final UserClubRepository userClubRepository;
    private final RestTemplate restTemplate;

    @Value("${strava.api.url}")
    private String stravaApiUrl;

    @Autowired
    public ClubsController(UserService userService, ClubRepository clubRepository, UserRepository userRepository,
                           UserClubRepository userClubRepository, RestTemplate restTemplate) {
        this.userService = userService;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.userClubRepository = userClubRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping(value = "/user/clubs")
    public ResponseEntity<?> getUserClubs(@RequestParam Long userId) {
        try {
            logger.info("Fetching clubs for user: {}", userId);

            // Get access token for the user
            String accessToken;
            try {
                accessToken = userService.getAccessToken(String.valueOf(userId));
            } catch (Exception e) {
                logger.error("Failed to get access token for user: {}", userId, e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Failed to retrieve access token"));
            }

            // Check if clubs already exist for this user
            List<UserClub> userClubs = userClubRepository.findById_UserId(userId);
            List<ClubDto> existingClubs = userClubs.stream()
                    .map(uc -> {
                        Club c = uc.getClub();
                        ClubDto dto = new ClubDto();
                        dto.setClubId(c.getClubId());
                        dto.setName(c.getName());
                        dto.setSportType(c.getSportType());
                        dto.setCity(c.getCity());
                        dto.setCountry(c.getCountry());
                        dto.setMemberCount(c.getMemberCount());
                        dto.setIsPrivate(c.getIsPrivate());
                        return dto;
                    })
                    .toList();

            if (!existingClubs.isEmpty()) {
                logger.info("Returning cached clubs for user: {}", userId);
                return ResponseEntity.ok(existingClubs);
            }

            // Call Strava API to get clubs
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<StravaClub[]> response = restTemplate.exchange(
                    stravaApiUrl + API_ATHLETE_CLUBS,
                    HttpMethod.GET,
                    entity,
                    StravaClub[].class
            );

            StravaClub[] stravaClubs = response.getBody();

            if (stravaClubs == null || stravaClubs.length == 0) {
                logger.info("No clubs found for user: {}", userId);
                return ResponseEntity.ok(new ArrayList<>());
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Save clubs to database
            List<Club> savedClubs = new ArrayList<>();
            for (StravaClub stravaClub : stravaClubs) {
                // Check if club already exists
                Club club = clubRepository.findById(stravaClub.getId()).orElseGet(() ->
                    Club.builder()
                            .clubId(stravaClub.getId())
                            .name(stravaClub.getName())
                            .city(stravaClub.getCity())
                            .country(stravaClub.getCountry())
                            .sportType(stravaClub.getSportType())
                            .isPrivate(stravaClub.getIsPrivate())
                            .memberCount(stravaClub.getMemberCount())
                            .profileImageUrl(stravaClub.getProfileMedium())
                            .coverPhotoUrl(stravaClub.getCoverPhoto())
                            .build()
                );

                Club savedClub = clubRepository.save(club);
                savedClubs.add(savedClub);

                // Save user-club relationship
                UserClub userClub = new UserClub();
                userClub.setUser(user);
                userClub.setClub(savedClub);
                userClub.setCreatedOn(LocalDateTime.now());

                userClubRepository.save(userClub);
            }

            logger.info("Saved {} clubs for user: {}", savedClubs.size(), userId);
            return ResponseEntity.ok(savedClubs);

        } catch (Exception e) {
            logger.error("Error fetching clubs for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clubs: " + e.getMessage()));
        }
    }
}




