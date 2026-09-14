package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.TestingRequirement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TestingRequirementRepository extends MongoRepository<TestingRequirement, String> {
    List<TestingRequirement> findByStandardId(String standardId);
}
