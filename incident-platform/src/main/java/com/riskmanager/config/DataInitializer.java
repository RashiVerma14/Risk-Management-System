package com.riskmanager.config;

import com.riskmanager.alert.AlertCondition;
import com.riskmanager.alert.AlertRule;
import com.riskmanager.alert.AlertRuleRepository;
import com.riskmanager.deployment.Deployment;
import com.riskmanager.deployment.DeploymentRepository;
import com.riskmanager.incident.CreateIncidentRequest;
import com.riskmanager.incident.IncidentResponseDto;
import com.riskmanager.incident.IncidentService;
import com.riskmanager.incident.Severity;
import com.riskmanager.rag.KnowledgeDocument;
import com.riskmanager.rag.KnowledgeDocumentRepository;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRepository;
import com.riskmanager.serviceregistry.ServiceStatus;
import com.riskmanager.user.Role;
import com.riskmanager.user.User;
import com.riskmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final DeploymentRepository deploymentRepository;
    private final IncidentService incidentService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            seedUsers();
            seedServices();
            seedAlertRules();
            seedKnowledgeDocuments();
            seedDeployments();
            seedDemoIncidents();
            log.info("RiskManager operational database seeding completed successfully.");
        } catch (Exception ex) {
            log.warn("Database seeding skipped or incomplete: {}", ex.getMessage());
        }
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        User admin = User.builder()
                .name("Alex SRE Admin")
                .email("admin@riskmanager.io")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User engineer = User.builder()
                .name("Jordan DevOps Engineer")
                .email("engineer@riskmanager.io")
                .password(passwordEncoder.encode("engineer123"))
                .role(Role.ENGINEER)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User viewer = User.builder()
                .name("Morgan Stakeholder Viewer")
                .email("viewer@riskmanager.io")
                .password(passwordEncoder.encode("viewer123"))
                .role(Role.VIEWER)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.saveAll(List.of(admin, engineer, viewer));
        log.info("Seeded 3 demo accounts: admin@riskmanager.io, engineer@riskmanager.io, viewer@riskmanager.io");
    }

    private void seedServices() {
        if (serviceRepository.count() > 0) return;

        Service notifService = Service.builder()
                .name("notification-service")
                .description("Dispatches emails, SMS, and push notifications")
                .ownerTeam("Messaging Team")
                .environment("production")
                .healthEndpoint("http://localhost:8080/actuator/health")
                .dependencies(new ArrayList<>())
                .repositoryUrl("https://github.com/org/notification-service")
                .version("1.2.0")
                .status(ServiceStatus.HEALTHY)
                .responseTimeMs(45L)
                .build();
        Service savedNotif = serviceRepository.save(notifService);

        Service authService = Service.builder()
                .name("auth-service")
                .description("Central Identity and OAuth2 Token Issuer")
                .ownerTeam("Security Team")
                .environment("production")
                .healthEndpoint("http://localhost:8080/actuator/health")
                .dependencies(new ArrayList<>())
                .repositoryUrl("https://github.com/org/auth-service")
                .version("3.1.0")
                .status(ServiceStatus.HEALTHY)
                .responseTimeMs(25L)
                .build();
        Service savedAuth = serviceRepository.save(authService);

        Service paymentService = Service.builder()
                .name("payment-service")
                .description("Card processing, Stripe/PayPal payment gateway integrations")
                .ownerTeam("Billing Core")
                .environment("production")
                .healthEndpoint("http://localhost:8080/actuator/health")
                .dependencies(List.of(savedNotif.getId()))
                .repositoryUrl("https://github.com/org/payment-service")
                .version("2.4.1")
                .status(ServiceStatus.HEALTHY)
                .responseTimeMs(110L)
                .build();
        Service savedPayment = serviceRepository.save(paymentService);

        Service orderService = Service.builder()
                .name("order-service")
                .description("Manages customer checkout and order fulfillments")
                .ownerTeam("Commerce Team")
                .environment("production")
                .healthEndpoint("http://localhost:8080/actuator/health")
                .dependencies(List.of(savedPayment.getId(), savedAuth.getId()))
                .repositoryUrl("https://github.com/org/order-service")
                .version("4.0.5")
                .status(ServiceStatus.HEALTHY)
                .responseTimeMs(75L)
                .build();
        Service savedOrder = serviceRepository.save(orderService);

        Service gatewayService = Service.builder()
                .name("api-gateway")
                .description("Reverse proxy, SSL termination, and rate-limiting gateway")
                .ownerTeam("Platform SRE")
                .environment("production")
                .healthEndpoint("http://localhost:8080/actuator/health")
                .dependencies(List.of(savedAuth.getId(), savedOrder.getId()))
                .repositoryUrl("https://github.com/org/api-gateway")
                .version("1.9.3")
                .status(ServiceStatus.HEALTHY)
                .responseTimeMs(15L)
                .build();
        serviceRepository.save(gatewayService);

        log.info("Seeded 5 interconnected production microservices.");
    }

    private void seedAlertRules() {
        if (alertRuleRepository.count() > 0) return;

        Service payment = serviceRepository.findByName("payment-service").orElse(null);
        Service order = serviceRepository.findByName("order-service").orElse(null);

        if (payment != null) {
            AlertRule rule1 = AlertRule.builder()
                    .name("Payment Gateway Error Spike")
                    .serviceId(payment.getId())
                    .metricName("error_rate")
                    .condition(AlertCondition.GREATER_THAN)
                    .threshold(15.0)
                    .severity(Severity.HIGH)
                    .cooldownMinutes(5)
                    .enabled(true)
                    .build();

            AlertRule rule2 = AlertRule.builder()
                    .name("Payment Downstream Timeout")
                    .serviceId(payment.getId())
                    .metricName("timeout_count")
                    .condition(AlertCondition.GREATER_THAN)
                    .threshold(5.0)
                    .severity(Severity.CRITICAL)
                    .cooldownMinutes(5)
                    .enabled(true)
                    .build();

            alertRuleRepository.saveAll(List.of(rule1, rule2));
        }

        if (order != null) {
            AlertRule rule3 = AlertRule.builder()
                    .name("Order Service Latency Surge")
                    .serviceId(order.getId())
                    .metricName("response_time_ms")
                    .condition(AlertCondition.GREATER_THAN)
                    .threshold(1000.0)
                    .severity(Severity.MEDIUM)
                    .cooldownMinutes(10)
                    .enabled(true)
                    .build();

            alertRuleRepository.save(rule3);
        }
        log.info("Seeded 3 operational alert rules.");
    }

    private void seedKnowledgeDocuments() {
        if (knowledgeDocumentRepository.count() > 0) return;

        Service payment = serviceRepository.findByName("payment-service").orElse(null);

        KnowledgeDocument doc1 = KnowledgeDocument.builder()
                .title("Runbook: Payment Service 504 Gateway Timeouts")
                .documentType("RUNBOOK")
                .serviceId(payment != null ? payment.getId() : null)
                .serviceName("payment-service")
                .content("""
                        SYMPTOMS:
                        - Sudden surge in 504 Gateway Timeouts from payment-service.
                        - Upstream order-service reports circuit breaker TRIP on /v1/charges.

                        ROOT CAUSE HYPOTHESES:
                        1. External payment gateway (Stripe/Adyen) API degradation or rate limit.
                        2. Database connection pool starvation due to unindexed transaction queries.
                        3. Recent deployment version incompatibility in retry/timeout configurations.

                        REMEDIATION STEPS:
                        1. Verify third-party status page for payment partner.
                        2. If recent deployment occurred in the last 30 minutes, initiate immediate canary rollback.
                        3. Check Redis cache connectivity for idempotency key lookups.
                        4. Scale payment-service replicas from 3 to 6 if CPU exceeds 80%.
                        """)
                .tags(List.of("payment", "timeout", "stripe", "circuit-breaker", "runbook"))
                .uploadedBy("sre-team@riskmanager.io")
                .createdAt(Instant.now().minus(Duration.ofDays(10)))
                .build();

        KnowledgeDocument doc2 = KnowledgeDocument.builder()
                .title("Postmortem: 2025 Q4 Connection Pool Exhaustion")
                .documentType("POSTMORTEM")
                .serviceId(payment != null ? payment.getId() : null)
                .serviceName("payment-service")
                .content("""
                        INCIDENT SUMMARY:
                        On 2025-11-14, payment-service failed to process transactions for 18 minutes.
                        ROOT CAUSE:
                        A code change removed @Transactional timeout, leaving stale read locks hanging on MongoDB replica sets.
                        RESOLUTION:
                        Enforced global query timeout of 3000ms and tuned Hikari/MongoDB connection pool maximum size to 100.
                        """)
                .tags(List.of("postmortem", "connection-pool", "database", "payment"))
                .uploadedBy("alex.admin@riskmanager.io")
                .createdAt(Instant.now().minus(Duration.ofDays(30)))
                .build();

        knowledgeDocumentRepository.saveAll(List.of(doc1, doc2));
        log.info("Seeded 2 RAG runbooks & postmortem knowledge documents.");
    }

    private void seedDeployments() {
        if (deploymentRepository.count() > 0) return;

        Service payment = serviceRepository.findByName("payment-service").orElse(null);
        if (payment != null) {
            Deployment dep = Deployment.builder()
                    .serviceId(payment.getId())
                    .serviceName(payment.getName())
                    .version("2.4.1")
                    .environment("production")
                    .deployedAt(Instant.now().minus(Duration.ofMinutes(12)))
                    .deployedBy("release-bot")
                    .commitHash("a8f3b9c")
                    .status("SUCCESS")
                    .changelog("Upgrade external HTTP client SDK and tune connection timeout settings")
                    .build();

            deploymentRepository.save(dep);
            log.info("Seeded recent deployment for payment-service v2.4.1 (12 mins ago).");
        }
    }

    private void seedDemoIncidents() {
        Service payment = serviceRepository.findByName("payment-service").orElse(null);
        if (payment == null) return;

        User engineer = userRepository.findByEmail("engineer@riskmanager.io").orElse(null);

        CreateIncidentRequest req = new CreateIncidentRequest(
                "Payment Service 504 Spike During Checkout",
                "Multiple checkout transactions failed with 504 Gateway Timeout on /api/v1/charge",
                Severity.HIGH,
                payment.getId(),
                payment.getId() + ":504_gateway_timeout",
                "Customer checkout failures increasing by 18%",
                List.of("payment", "checkout", "504"),
                List.of(
                        "ERROR [payment-service] Connection timeout connecting to payment-gateway after 3000ms",
                        "WARN [order-service] Circuit breaker tripped on endpoint /charge"
                )
        );

        try {
            IncidentResponseDto incident = incidentService.createIncident(req, "SYSTEM_SEED");
            if (engineer != null) {
                incidentService.assignIncident(incident.getId(), engineer.getId(), "SYSTEM_SEED");
            }
            log.info("Seeded active demo incident ID: {}", incident.getId());
        } catch (Exception ex) {
            log.warn("Demo incident creation skipped: {}", ex.getMessage());
        }
    }
}
