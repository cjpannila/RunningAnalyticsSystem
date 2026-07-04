package com.cjpannila.runanalytics.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clubs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "sport_type", length = 50)
    private String sportType;

    @Column(name = "private")
    private Boolean isPrivate;

    @Column(name = "member_count")
    private Integer memberCount;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "cover_photo_url", columnDefinition = "TEXT")
    private String coverPhotoUrl;
}

