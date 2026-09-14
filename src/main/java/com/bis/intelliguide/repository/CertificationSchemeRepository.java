package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.CertificationScheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CertificationSchemeRepository extends MongoRepository<CertificationScheme, String> {
    Page<CertificationScheme> findByStatus(String status, Pageable pageable);
}
