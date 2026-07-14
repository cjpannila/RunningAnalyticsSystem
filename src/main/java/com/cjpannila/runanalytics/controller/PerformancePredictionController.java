package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.service.FeatureEngineeringService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformancePredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PerformancePredictionController.class);

    private final FeatureEngineeringService featureEngineeringService;

    @GetMapping("/training-dataset")
    public ResponseEntity<byte[]> generateTrainingDataset() {
        try {
            logger.info("Training dataset export requested");
            var result = featureEngineeringService.generateTrainingDatasetCsv();

            byte[] csvBytes = result.getCsvBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "training_dataset.csv");
            headers.setContentLength(csvBytes.length);

            headers.add("X-Rows-Generated", String.valueOf(result.getRowsGenerated()));
            headers.add("X-Activities-Used", String.valueOf(result.getActivitiesUsed()));

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Failed to generate training dataset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
