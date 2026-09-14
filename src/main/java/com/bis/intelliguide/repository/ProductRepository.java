package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
