package com.riskmanager.rag;

import com.riskmanager.serviceregistry.ServiceRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private ServiceRegistryService serviceRegistryService;

    @InjectMocks
    private RagService ragService;

    private KnowledgeDocument runbookPayment;
    private KnowledgeDocument postmortemAuth;
    private KnowledgeDocument guideGeneral;

    @BeforeEach
    void setUp() {
        runbookPayment = KnowledgeDocument.builder()
                .id("doc-1")
                .title("Payment Gateway 504 Timeout Runbook")
                .content("If payment gateway responds with 504 timeout, restart connection pool and check downstream PSP.")
                .documentType("RUNBOOK")
                .serviceId("srv-payment")
                .tags(List.of("payment", "gateway", "timeout"))
                .createdAt(Instant.now())
                .build();

        postmortemAuth = KnowledgeDocument.builder()
                .id("doc-2")
                .title("Auth Token Validation High Latency Postmortem")
                .content("JWT verification failed due to Redis cluster failover and high latency.")
                .documentType("POSTMORTEM")
                .serviceId("srv-auth")
                .tags(List.of("auth", "jwt", "redis"))
                .createdAt(Instant.now())
                .build();

        guideGeneral = KnowledgeDocument.builder()
                .id("doc-3")
                .title("General On-Call Escalation Guidelines")
                .content("Contact SRE lead if incident remains unmitigated after 30 minutes.")
                .documentType("GENERAL")
                .serviceId("srv-global")
                .tags(List.of("oncall", "sre"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testRetrieveRelevantDocumentsExactServiceAndQueryMatch() {
        when(knowledgeDocumentRepository.findAll()).thenReturn(List.of(runbookPayment, postmortemAuth, guideGeneral));

        List<KnowledgeDocument> results = ragService.retrieveRelevantDocuments("payment timeout errors", "srv-payment", 5);

        assertFalse(results.isEmpty());
        assertEquals("doc-1", results.get(0).getId());
        assertEquals("Payment Gateway 504 Timeout Runbook", results.get(0).getTitle());
    }

    @Test
    void testRetrieveRelevantDocumentsFallBackKeywordScoring() {
        when(knowledgeDocumentRepository.findAll()).thenReturn(List.of(runbookPayment, postmortemAuth, guideGeneral));

        List<KnowledgeDocument> results = ragService.retrieveRelevantDocuments("jwt redis cluster failure", null, 5);

        assertFalse(results.isEmpty());
        assertEquals("doc-2", results.get(0).getId());
        assertEquals("POSTMORTEM", results.get(0).getDocumentType());
    }

    @Test
    void testRetrieveRelevantDocumentsReturnsEmptyOnUnrelatedQuery() {
        when(knowledgeDocumentRepository.findAll()).thenReturn(List.of(runbookPayment, postmortemAuth, guideGeneral));

        List<KnowledgeDocument> results = ragService.retrieveRelevantDocuments("quantum gravity astrophysics", "srv-unknown", 5);

        assertTrue(results.isEmpty());
    }
}
