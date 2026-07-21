package com.cjpannila.runanalytics.repositories;

import com.cjpannila.runanalytics.entities.Club;
import com.cjpannila.runanalytics.entities.User;
import com.cjpannila.runanalytics.entities.UserClub;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserClubRepository extends CrudRepository<UserClub, UserClub.UserClubId> {
    List<UserClub> findById_UserId(Long userId);

    List<UserClub> findById_ClubId(Long clubId);

    Optional<UserClub> findByUserAndClub(User user, Club club);
}

