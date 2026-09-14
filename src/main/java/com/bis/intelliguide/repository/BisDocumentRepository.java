package com.bis.intelliguide.repository;

import com.bis.intelliguide.model.BisDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BisDocumentRepository extends MongoRepository<BisDocument, String> {
}
