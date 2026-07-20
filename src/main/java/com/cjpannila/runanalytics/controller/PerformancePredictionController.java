package com.cjpannila.runanalytics.controller;

import com.cjpannila.runanalytics.dto.PredictionResponseDto;
import com.cjpannila.runanalytics.dto.PredictionTableRowDto;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cjpannila.runanalytics.util.Constants.DOWNLOADS;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformancePredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PerformancePredictionController.class);
    private static final String PY_TRAINING_BASE = "http://127.0.0.1:8001";

    private final FeatureEngineeringService featureEngineeringService;
    private final RestTemplate restTemplate;

    @GetMapping(value = "/training-dataset", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateTrainingDataset(@RequestParam(required = false) Long userId) {
        try {
            logger.info("Training dataset export requested");
            TrainingDatasetExportResultDto result = featureEngineeringService.generateTrainingDatasetCsv(userId);

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
            //skip empty hr and cadence records - false
            TrainingDatasetExportResultDto result = featureEngineeringService.saveWeeklySummary(false);
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

    @GetMapping(value = "/prediction-rows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PredictionTableRowDto>> getPredictionRows(@RequestParam(defaultValue = "true") boolean limit,
                                                                          @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(featureEngineeringService.buildPredictionRows(limit, false, userId));
    }

    @GetMapping(value = "/predict", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PredictionResponseDto> predict(@RequestParam(defaultValue = "target_next_week_km") String target,
                                                         @RequestParam(defaultValue = "true") boolean limit,
                                                         @RequestParam(name = "model_type", defaultValue = Constants.RANDOM_FOREST) String modelType,
                                                         @RequestParam(required = false) Long userId) {
        try {
            logger.info("Prediction requested for target={}", target);

            TrainingDatasetExportResultDto datasetResult = featureEngineeringService.savePredictionDataset(limit, userId);
            if (datasetResult.getCsvBytes() == null || datasetResult.getCsvBytes().length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(PredictionResponseDto.builder()
                                .message("Error: no prediction dataset content was generated")
                                .target(target)
                                .build());
            }

            ResponseEntity<List<Map<String, Object>>> pythonResponse = restTemplate.exchange(
                    PY_TRAINING_BASE + "/predict?target=" + target + "&model_type=" + modelType,
                    org.springframework.http.HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> predictions = pythonResponse.getBody() == null ? List.of() : pythonResponse.getBody();

            PredictionResponseDto result = new PredictionResponseDto();
            result.setMessage("Predictions generated successfully");
            result.setTarget(target);
            result.setDatasetPath(resolvePredictionDatasetFile().toAbsolutePath().toString());
            result.setRowsGenerated(datasetResult.getRowsGenerated());
            result.setPredictions(getShiftedPredictions(predictions, target));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to generate predictions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PredictionResponseDto.builder()
                            .message("Error")
                            .target(target)
                            .build());
        }
    }

    //Shift predictions to reflect next week with the same user_id and target prediction value
    private List<Map<String, Object>> getShiftedPredictions(List<Map<String, Object>> predictions, String target) {
        List<Map<String, Object>> shiftedPredictions = new ArrayList<>();
        for (Map<String, Object> prediction : predictions) {
            Map<String, Object> shiftedPrediction = new HashMap<>(prediction);
            String currentUserId = prediction.get("user_id").toString();
            String currentWeekStart = prediction.get("week_start").toString();
            shiftedPrediction.put("user_id", currentUserId);
            shiftedPrediction.put(target + "_prediction", prediction.get(target + "_prediction"));
            shiftedPrediction.put("week_start", getNextWeekStartDate(predictions, currentWeekStart, Integer.valueOf(currentUserId)));
            shiftedPredictions.add(shiftedPrediction);
        }
        return shiftedPredictions;
    }

    private String getNextWeekStartDate(List<Map<String, Object>> predictions, String currentWeekStart, Integer currentUserId) {
        LocalDate weekStartDate = LocalDate.parse(currentWeekStart, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate nextWeekStartDate = weekStartDate.plusWeeks(1);
        LocalDate lastMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        boolean nextWeekExists = predictions.stream().anyMatch(p ->
                currentUserId.equals(((Number) p.get("user_id")).intValue()) &&
                        nextWeekStartDate.toString().equals(p.get("week_start"))
        );
        if (nextWeekExists) {
            return nextWeekStartDate.toString();
        } else {
            if (nextWeekStartDate.isBefore(lastMonday)) {
                return getNextWeekStartDate(predictions, nextWeekStartDate.toString(), currentUserId);
            } else {
                return nextWeekStartDate.toString();
            }
        }
    }

    private Path resolveDownloadsFile(String fileName) {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path downloadsDir = homeDir.resolve(DOWNLOADS);
        Path targetDir = Files.exists(downloadsDir) ? downloadsDir : homeDir;
        return targetDir.resolve(fileName);
    }

    private Path resolvePredictionDatasetFile() {
        return resolveDownloadsFile(Constants.PREDICTION_DATASET_FILE_NAME);
    }

    private Path resolveTrainingDatasetOutputFile() {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path downloadsDir = homeDir.resolve(DOWNLOADS);
        Path targetDir = Files.exists(downloadsDir) ? downloadsDir : homeDir;
        return targetDir.resolve(Constants.TRAINING_DATASET_FILE_NAME);
    }
}
