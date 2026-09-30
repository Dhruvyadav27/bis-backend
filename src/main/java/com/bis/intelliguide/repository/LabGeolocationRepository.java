package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.LabGeolocation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LabGeolocationRepository extends MongoRepository<LabGeolocation, String> {
    List<LabGeolocation> findByLabNameIn(List<String> labNames);
    Optional<LabGeolocation> findByLabName(String labName);
    boolean existsByLabName(String labName);
}
