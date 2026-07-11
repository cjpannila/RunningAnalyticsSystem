package com.cjpannila.runanalytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StravaClub {

    private Long id;

    @JsonProperty("resource_state")
    private Integer resourceState;

    private String name;

    private String city;

    private String state;

    private String country;

    @JsonProperty("private")
    private Boolean isPrivate;

    @JsonProperty("member_count")
    private Integer memberCount;

    @JsonProperty("featured")
    private Boolean featured;

    @JsonProperty("verified")
    private Boolean verified;

    @JsonProperty("url")
    private String url;

    @JsonProperty("membership")
    private String membership;

    @JsonProperty("admin")
    private Boolean admin;

    @JsonProperty("owner")
    private Boolean owner;

    @JsonProperty("following")
    private Boolean following;

    @JsonProperty("profile_medium")
    private String profileMedium;

    @JsonProperty("profile")
    private String profile;

    @JsonProperty("cover_photo")
    private String coverPhoto;

    @JsonProperty("cover_photo_small")
    private String coverPhotoSmall;

    @JsonProperty("sport_type")
    private String sportType;
}

