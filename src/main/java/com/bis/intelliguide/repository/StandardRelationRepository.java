package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.StandardRelation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StandardRelationRepository extends MongoRepository<StandardRelation, String> {
    List<StandardRelation> findByStandardId(String standardId);
}
