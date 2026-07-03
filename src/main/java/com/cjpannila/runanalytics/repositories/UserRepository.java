package com.cjpannila.runanalytics.repositories;

import com.cjpannila.runanalytics.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByFirstnameAndLastname(String firstname, String lastname);
}
