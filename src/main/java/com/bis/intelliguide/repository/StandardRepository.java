package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.Standard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StandardRepository extends MongoRepository<Standard, String> {
    Page<Standard> findByStatus(String status, Pageable pageable);
    Page<Standard> findByStatusAndCategory(String status, String category, Pageable pageable);
    List<Standard> findByStatusAndTitleContainingIgnoreCase(String status, String title);
}
