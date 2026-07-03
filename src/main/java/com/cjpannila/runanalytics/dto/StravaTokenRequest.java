package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StravaTokenRequest {
    private String client_id;
    private String client_secret;
    private String refresh_token;
    private String code;
    private String grant_type;
}

