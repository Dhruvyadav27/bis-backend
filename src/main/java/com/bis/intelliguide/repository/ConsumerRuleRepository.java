package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.ConsumerRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConsumerRuleRepository extends MongoRepository<ConsumerRule, String> {
    Page<ConsumerRule> findByStatus(String status, Pageable pageable);
    Optional<ConsumerRule> findByActNameAndClauseRef(String actName, String clauseRef);
}
