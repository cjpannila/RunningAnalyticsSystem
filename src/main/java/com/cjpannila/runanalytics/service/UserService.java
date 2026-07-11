package com.cjpannila.runanalytics.service;

import com.cjpannila.runanalytics.dto.StravaTokenResponse;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Value("${strava.client.id}")
    private String stravaClientId;

    @Value("${strava.client.secret}")
    private String stravaClientSecret;

    @Value("${strava.oauth.token.url}")
    private String stravaTokenUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public UserService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    public User loadUserByUsername(String firstname, String lastname) throws Exception {
        Optional<User> user = userRepository.findByFirstnameAndLastname(firstname, lastname);
        return user.orElseThrow(() -> new Exception("user not found " + firstname + " " + lastname));
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public String getAccessToken(@RequestParam String userId) throws Exception {
        try {
            Long uid = Long.parseLong(userId);
            Optional<User> userOptional = userRepository.findById(uid);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                Long expiresAt = user.getTokenExpiresAt();
                long nowSecs = System.currentTimeMillis() / 1000L;
                if (expiresAt != null && expiresAt > nowSecs && user.getAccessToken() != null) {
                    // token still valid
                    logger.info("Using existing access token for user: {}", user.getUserId());
                    return user.getAccessToken();
                } else {
                    // need to refresh
                    if (user.getRefreshToken() == null) {
                        throw new Exception("refresh token is null");
                    } else {
                        // prepare form data for refresh token grant
                        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                        form.add("client_id", stravaClientId);
                        form.add("client_secret", stravaClientSecret);
                        form.add("grant_type", "refresh_token");
                        form.add("refresh_token", user.getRefreshToken());

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

                        StravaTokenResponse tokenResponse = restTemplate.postForObject(
                                stravaTokenUrl,
                                request,
                                StravaTokenResponse.class
                        );

                        if (tokenResponse == null) {
                            throw new Exception("token response is null");
                        }
                        // update user tokens
                        user.setAccessToken(tokenResponse.getAccessToken());
                        user.setRefreshToken(tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken() : user.getRefreshToken());
                        user.setTokenExpiresAt(tokenResponse.getExpiresAt());
                        User saved = userRepository.save(user);
                        logger.info("Access token refreshed for user: {}", saved.getUserId());
                        return saved.getAccessToken();
                    }
                }
            } else {
                throw new Exception("user not found " + userId);
            }
        } catch (NumberFormatException nfe) {
            throw  new IllegalArgumentException("Invalid userId: " + userId, nfe);
        } catch (Exception e) {
            logger.error("Error refreshing access token", e);
            throw new Exception("Failed to retrieve or refresh access token", e);
        }
    }
}
