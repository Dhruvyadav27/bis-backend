package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.UserJourneyProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserJourneyProgressRepository extends MongoRepository<UserJourneyProgress, String> {
    Optional<UserJourneyProgress> findTopByUserIdOrderByUpdatedAtDesc(String userId);
}
