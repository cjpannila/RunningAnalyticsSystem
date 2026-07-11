package com.cjpannila.runanalytics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserClubDto {
    private Long clubId;
    private String name;
    private String sportType;
    private String city;
    private String country;
    private Integer memberCount;
    private Boolean isPrivate;
}
