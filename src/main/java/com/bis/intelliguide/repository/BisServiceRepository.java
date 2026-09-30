package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.BisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BisServiceRepository extends MongoRepository<BisService, String> {
    Page<BisService> findByStatus(String status, Pageable pageable);
    Optional<BisService> findByServiceName(String serviceName);
}