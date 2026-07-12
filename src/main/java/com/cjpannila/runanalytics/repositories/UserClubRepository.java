package com.cjpannila.runanalytics.repositories;

import com.cjpannila.runanalytics.entities.UserClub;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserClubRepository extends CrudRepository<UserClub, UserClub.UserClubId> {
    List<UserClub> findById_UserId(Long userId);

    List<UserClub> findById_ClubId(Long clubId);
}

