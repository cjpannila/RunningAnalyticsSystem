package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.dto.ClubWeeklyStatsResponseDto;
import com.cjpannila.runanalytics.dto.UserClubDto;
import com.cjpannila.runanalytics.entities.Club;
import com.cjpannila.runanalytics.entities.UserClub;
import com.cjpannila.runanalytics.repositories.UserClubRepository;
import com.cjpannila.runanalytics.dto.ClubMemberDto;
import com.cjpannila.runanalytics.service.ClubLeaderboardService;
import com.cjpannila.runanalytics.service.ClubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ClubController {

    Logger logger = LoggerFactory.getLogger(ClubController.class);

    private final ClubService clubService;
    private final UserClubRepository userClubRepository;
    private final ClubLeaderboardService clubLeaderboardService;

    @Autowired
    public ClubController(ClubService clubService,
                          UserClubRepository userClubRepository,
                          ClubLeaderboardService clubLeaderboardService) {
        this.clubService = clubService;
        this.userClubRepository = userClubRepository;
        this.clubLeaderboardService = clubLeaderboardService;
    }

    @GetMapping(value = "/clubs/{clubId}/members")
    public ResponseEntity<?> getClubMembers(@PathVariable Long clubId) {
        try {
            List<UserClub> userClubs = userClubRepository.findById_ClubId(clubId);
            List<ClubMemberDto> members = new ArrayList<>();
            for (UserClub uc : userClubs) {
                if (uc.getUser() == null) continue;
                members.add(ClubMemberDto.builder()
                        .userId(uc.getUser().getUserId())
                        .firstname(uc.getUser().getFirstname())
                        .lastname(uc.getUser().getLastname())
                        .build());
            }
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            logger.error("Error fetching club members for club: {}", clubId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch club members"));
        }
    }

    /**
     * This is used to show club dashboard leaderboards for a specific week.
     * @param clubId of the club
     * @param weekStart The start date of the week
     * @return ResponseEntity<ClubWeeklyStatsResponseDto> with List<ClubMemberWeeklyStatsDto> members
     */
    @GetMapping(value = "/clubs/{clubId}/weekly-stats")
    public ResponseEntity<?> getClubWeeklyStats(@PathVariable Long clubId,
                                                @RequestParam(required = false) String weekStart) {
        try {
            LocalDate selectedWeekStart = resolveWeekStart(weekStart);
            ClubWeeklyStatsResponseDto stats = clubLeaderboardService.getWeeklyStats(clubId, selectedWeekStart);
            return ResponseEntity.ok(stats);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid weekStart format. Use YYYY-MM-DD."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching weekly stats for club: {}", clubId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch club weekly stats"));
        }
    }

    private LocalDate resolveWeekStart(String weekStart) {
        if (weekStart == null || weekStart.isBlank()) {
            return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate requestedDate = LocalDate.parse(weekStart);
        return requestedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Get user clubs from clubs table if exists for this userId
     * else call Strava API to get clubs and save to clubs table and user_clubs table.
     * @param userId
     * @return
     */
    @GetMapping(value = "/user/clubs")
    public ResponseEntity<?> getUserClubs(@RequestParam Long userId) {
        try {
            logger.info("Fetching clubs for user: {}", userId);

            // Check if clubs already exist for this user
            List<UserClub> userClubs = userClubRepository.findById_UserId(userId);
            List<UserClubDto> existingClubs = userClubs.stream()
                    .map(uc -> {
                        Club c = uc.getClub();
                        UserClubDto dto = new UserClubDto();
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

            // No saved clubs, call Strava API to get clubs
            return clubService.callClubsApiAndSavetoDB(userId);

        } catch (Exception e) {
            logger.error("Error fetching clubs for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clubs: " + e.getMessage()));
        }
    }

    /**
     * Get all clubs from clubs table
     * @return
     */
    @GetMapping(value = "/clubs")
    public ResponseEntity<?> getAllClubs() {
        try {
            logger.info("Fetching all clubs");
            List<UserClubDto> clubs = clubService.getAllClubs();
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            logger.error("Error fetching all clubs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clubs: " + e.getMessage()));
        }
    }
}