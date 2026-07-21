package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.dto.StravaClubDto;
import com.cjpannila.runanalytics.dto.UserClubDto;
import com.cjpannila.runanalytics.entities.Club;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.entities.UserClub;
import com.cjpannila.runanalytics.repositories.ClubRepository;
import com.cjpannila.runanalytics.repositories.UserClubRepository;
import com.cjpannila.runanalytics.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.client.RestTemplate;

import static com.cjpannila.runanalytics.util.Constants.API_ATHLETE_CLUBS;

@Service
public class ClubService {

    Logger logger = LoggerFactory.getLogger(ClubService.class);

    private final UserService userService;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final UserClubRepository userClubRepository;
    private final RestTemplate restTemplate;
    @Value("${strava.api.url}")
    private String stravaApiUrl;

    public ClubService(UserService userService, ClubRepository clubRepository, UserRepository userRepository,
                       UserClubRepository userClubRepository, RestTemplate restTemplate) {
        this.userService = userService;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.userClubRepository = userClubRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Gets user access token and calls strava api to get clubs and save to DB
     * @param userId
     * @return
     */
    @Nonnull
    public ResponseEntity<?> callClubsApiAndSavetoDB(Long userId) {
        // Get access token for the user first
        String accessToken;
        try {
            accessToken = userService.getAccessToken(String.valueOf(userId));
        } catch (Exception e) {
            logger.error("Failed to get access token for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Failed to retrieve access token"));
        }
        // Call strava api /athlete/clubs
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<StravaClubDto[]> response = restTemplate.exchange(
                stravaApiUrl + API_ATHLETE_CLUBS,
                HttpMethod.GET,
                entity,
                StravaClubDto[].class
        );
        StravaClubDto[] stravaClubDtos = response.getBody();

        if (stravaClubDtos == null || stravaClubDtos.length == 0) {
            logger.info("No clubs found for user: {}", userId);
            return ResponseEntity.ok(new ArrayList<>());
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save clubs to database
        List<Club> savedClubs = new ArrayList<>();
        // Loop through stravaClubDtos returned from Strava api
        for (StravaClubDto stravaClubDto : stravaClubDtos) {
            // Check if club already exists
            Club club = clubRepository.findById(stravaClubDto.getId()).orElseGet(() ->
                    Club.builder()
                            .clubId(stravaClubDto.getId())
                            .name(stravaClubDto.getName())
                            .city(stravaClubDto.getCity())
                            .country(stravaClubDto.getCountry())
                            .sportType(stravaClubDto.getSportType())
                            .isPrivate(stravaClubDto.getIsPrivate())
                            .memberCount(stravaClubDto.getMemberCount())
                            .profileImageUrl(stravaClubDto.getProfileMedium())
                            .coverPhotoUrl(stravaClubDto.getCoverPhoto())
                            .build()
            );
            Club savedClub = clubRepository.save(club);
            savedClubs.add(savedClub);

            Optional<UserClub> existingUserClubs = userClubRepository.findByUserAndClub(user, savedClub);
            if (existingUserClubs.isEmpty()) {
                // Save user-club relationship
                UserClub userClub = new UserClub();
                userClub.setUser(user);
                userClub.setClub(savedClub);
                userClub.setCreatedOn(LocalDateTime.now());
                userClubRepository.save(userClub);
            }
        }
        logger.info("Saved {} clubs for user: {}", savedClubs.size(), userId);
        return ResponseEntity.ok(savedClubs);
    }

    public List<UserClubDto> getAllClubs() {
        List<Club> clubs = new ArrayList<>();
        clubRepository.findAll().forEach(clubs::add);
        List<UserClubDto> clubDtos = new ArrayList<>();
        clubs.forEach(club -> {
            UserClubDto dto = new UserClubDto();
            dto.setClubId(club.getClubId());
            dto.setName(club.getName());
            dto.setSportType(club.getSportType());
            dto.setCity(club.getCity());
            dto.setCountry(club.getCountry());
            dto.setMemberCount(club.getMemberCount());
            dto.setIsPrivate(club.getIsPrivate());
            clubDtos.add(dto);
        });
        return clubDtos;
    }
}
