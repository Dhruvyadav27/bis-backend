package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.BisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BisServiceRepository extends MongoRepository<BisService, String> {
    Page<BisService> findByStatus(String status, Pageable pageable);
}
