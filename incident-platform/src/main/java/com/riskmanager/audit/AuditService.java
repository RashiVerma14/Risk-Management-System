package com.riskmanager.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String actor, String action, String resource, String resourceId, Map<String, Object> details, String ip) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actor(actor != null ? actor : "SYSTEM")
                    .action(action)
                    .resource(resource)
                    .resourceId(resourceId)
                    .ipAddress(ip)
                    .details(details != null ? details : Map.of())
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded: {} by {} on {}", action, actor, resource);
        } catch (Exception ex) {
            log.warn("Failed to write audit log: {}", ex.getMessage());
        }
    }

    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findAll(PageRequest.of(0, limit)).getContent();
    }
}
