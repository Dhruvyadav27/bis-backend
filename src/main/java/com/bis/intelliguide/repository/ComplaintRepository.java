package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    Optional<Complaint> findByReferenceId(String referenceId);
    Page<Complaint> findByStatus(String status, Pageable pageable);
}
