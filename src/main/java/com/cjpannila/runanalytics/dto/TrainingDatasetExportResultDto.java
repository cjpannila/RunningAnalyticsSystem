package com.cjpannila.runanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDatasetExportResultDto {
    private byte[] csvBytes;
    private int rowsGenerated;
    private long activitiesUsed;
}

