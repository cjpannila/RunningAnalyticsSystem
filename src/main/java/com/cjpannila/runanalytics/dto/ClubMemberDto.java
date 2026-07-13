package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMemberDto {
    private Long userId;
    private String firstname;
    private String lastname;
}

