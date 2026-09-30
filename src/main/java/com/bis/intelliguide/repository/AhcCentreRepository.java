package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.AhcCentre;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AhcCentreRepository extends MongoRepository<AhcCentre, String> {
    long count();
}