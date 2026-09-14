package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.ProductStandard;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductStandardRepository extends MongoRepository<ProductStandard, String> {
    List<ProductStandard> findByProductId(String productId);
}
