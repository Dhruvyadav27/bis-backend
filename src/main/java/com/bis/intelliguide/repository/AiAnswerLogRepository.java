package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.AiAnswerLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiAnswerLogRepository extends MongoRepository<AiAnswerLog, String> {
    Page<AiAnswerLog> findByConfidenceScoreBetween(double min, double max, Pageable pageable);
    Page<AiAnswerLog> findByFlaggedTrue(Pageable pageable);
}
