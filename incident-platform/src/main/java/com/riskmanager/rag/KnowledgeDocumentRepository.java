package com.riskmanager.rag;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findByServiceId(String serviceId);

    List<KnowledgeDocument> findByDocumentType(String documentType);

    List<KnowledgeDocument> findByTitleContainingIgnoreCase(String keyword);
}
