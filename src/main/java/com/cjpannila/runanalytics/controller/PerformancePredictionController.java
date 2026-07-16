package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.dto.TrainingDatasetExportResultDto;
import com.cjpannila.runanalytics.service.FeatureEngineeringService;
import com.cjpannila.runanalytics.util.Constants;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformancePredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PerformancePredictionController.class);

    private final FeatureEngineeringService featureEngineeringService;

    @GetMapping(value = "/training-dataset", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateTrainingDataset() {
        try {
            logger.info("Training dataset export requested");
            var result = featureEngineeringService.generateTrainingDatasetCsv();

            byte[] csvBytes = result.getCsvBytes();

            if (csvBytes == null || csvBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: no training dataset content was generated");
            }

            Path outputFile = resolveTrainingDatasetOutputFile();
            Files.createDirectories(outputFile.getParent());
            Files.write(
                    outputFile,
                    csvBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Rows-Generated", String.valueOf(result.getRowsGenerated()));
            headers.add("X-Activities-Used", String.valueOf(result.getActivitiesUsed()));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Saved training dataset to Downloads/" + Constants.TRAINING_DATASET_FILE_NAME);
        } catch (Exception e) {
            logger.error("Failed to generate training dataset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    @GetMapping(value = "/save-weekly-summary", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> saveWeeklySummary() {
        try {
            logger.info("Weekly summary save requested");
            TrainingDatasetExportResultDto result = featureEngineeringService.saveWeeklySummary();
            if (result.getRowsGenerated() > 0) {
                return ResponseEntity.ok("Saved weekly summary, "
                        + result.getRowsGenerated() + " records were saved");
            }
            return ResponseEntity.status(HttpStatus.OK).body("No new weekly summary records were saved");
        } catch (Exception e) {
            logger.error("Failed to save weekly summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    private Path resolveTrainingDatasetOutputFile() {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path downloadsDir = homeDir.resolve("Downloads");
        Path targetDir = Files.exists(downloadsDir) ? downloadsDir : homeDir;
        return targetDir.resolve(Constants.TRAINING_DATASET_FILE_NAME);
    }
}
