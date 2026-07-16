package com.cjpannila.runanalytics.repositories;

import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.entities.WeeklySummary;
import com.cjpannila.runanalytics.entities.WeeklySummary.WeeklySummaryId;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklySummaryRepository extends CrudRepository<WeeklySummary, WeeklySummaryId> {

    Optional<WeeklySummary> findByUserAndWeekStart(User user, LocalDate weekStart);

    List<WeeklySummary> findByUser_UserIdOrderByWeekStartDesc(Long userId);
}