package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.HuidRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface HuidRecordRepository extends MongoRepository<HuidRecord, String> {
    Optional<HuidRecord> findByHuid(String huid);
}
