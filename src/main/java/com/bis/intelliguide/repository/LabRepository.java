package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.Lab;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LabRepository extends MongoRepository<Lab, String> {
    List<Lab> findByStateIgnoreCaseAndDistrictIgnoreCase(String state, String district);
    List<Lab> findByStateIgnoreCase(String state);
}
