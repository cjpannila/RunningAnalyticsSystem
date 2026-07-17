package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponseDto {
    private String message;
    private String target;
    private String datasetPath;
    private Integer rowsGenerated;
    private List<Map<String, Object>> predictions;
}


