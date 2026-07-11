package com.cjpannila.runanalytics.repositories;

import com.cjpannila.runanalytics.entities.Activity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ActivityRepository extends CrudRepository<Activity, Long> {
    List<Activity> findByUser_UserId(Long userId);

    java.util.Optional<Activity> findTopByUser_UserIdOrderByStartTimeDesc(Long userId);
}

