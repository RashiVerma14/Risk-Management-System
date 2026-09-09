package com.riskmanager.incident;

import com.riskmanager.common.PageResponse;
import com.riskmanager.exception.BadRequestException;
import com.riskmanager.exception.ResourceNotFoundException;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import com.riskmanager.user.Role;
import com.riskmanager.user.User;
import com.riskmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final ServiceRegistryService serviceRegistryService;
    private final UserRepository userRepository;
    private final IncidentStateMachine stateMachine;

    public IncidentResponseDto createIncident(CreateIncidentRequest request, String actor) {
        Service service = serviceRegistryService.findServiceEntityById(request.affectedServiceId());

        String correlationKey = request.correlationKey();
        if (correlationKey == null || correlationKey.isBlank()) {
            correlationKey = service.getId() + ":" + request.title().trim().toLowerCase();
        }

        // Incident Correlation / Deduplication: Check for active incident with same correlationKey
        List<IncidentStatus> resolvedStatuses = List.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);
        Optional<Incident> existingActiveOpt = incidentRepository
                .findTopByCorrelationKeyAndStatusNotInOrderByCreatedAtDesc(correlationKey, resolvedStatuses);

        if (existingActiveOpt.isPresent()) {
            Incident existing = existingActiveOpt.get();
            existing.setEventCount(existing.getEventCount() + 1);
            if (request.logs() != null && !request.logs().isEmpty()) {
                if (existing.getLogs() == null) existing.setLogs(new ArrayList<>());
                existing.getLogs().addAll(request.logs());
            }
            existing.setUpdatedAt(Instant.now());
            Incident saved = incidentRepository.save(existing);

            createTimelineEvent(
                    saved.getId(),
                    EventType.CORRELATED_EVENT_ADDED,
                    "Correlated duplicate event received (total events: " + saved.getEventCount() + ")",
                    actor != null ? actor : "SYSTEM",
                    "CORRELATION_ENGINE",
                    Map.of("correlationKey", correlationKey, "count", saved.getEventCount())
            );

            log.info("Correlated event with existing incident {} (key: {})", saved.getId(), correlationKey);
            return mapToDto(saved);
        }

        Instant now = Instant.now();
        Incident incident = Incident.builder()
                .title(request.title())
                .description(request.description())
                .severity(request.severity())
                .status(IncidentStatus.OPEN)
                .affectedServiceId(service.getId())
                .correlationKey(correlationKey)
                .eventCount(1)
                .impact(request.impact())
                .tags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>())
                .logs(request.logs() != null ? new ArrayList<>(request.logs()) : new ArrayList<>())
                .detectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Incident saved = incidentRepository.save(incident);

        createTimelineEvent(
                saved.getId(),
                EventType.INCIDENT_CREATED,
                "Incident opened with severity: " + saved.getSeverity(),
                actor != null ? actor : "SYSTEM",
                "INCIDENT_SERVICE",
                Map.of("severity", saved.getSeverity().name(), "service", service.getName())
        );

        log.info("Created new incident {} for service {}", saved.getId(), service.getName());
        return mapToDto(saved);
    }

    public PageResponse<IncidentResponseDto> getIncidents(
            IncidentStatus status,
            Severity severity,
            String serviceId,
            String engineerId,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Incident> incidentPage = incidentRepository.findAll(pageable);

        List<IncidentResponseDto> filtered = incidentPage.getContent().stream()
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> severity == null || i.getSeverity() == severity)
                .filter(i -> serviceId == null || serviceId.equalsIgnoreCase(i.getAffectedServiceId()))
                .filter(i -> engineerId == null || engineerId.equalsIgnoreCase(i.getAssignedEngineerId()))
                .map(this::mapToDto)
                .toList();

        return PageResponse.<IncidentResponseDto>builder()
                .content(filtered)
                .pageNumber(incidentPage.getNumber())
                .pageSize(incidentPage.getSize())
                .totalElements(incidentPage.getTotalElements())
                .totalPages(incidentPage.getTotalPages())
                .last(incidentPage.isLast())
                .build();
    }

    public IncidentResponseDto getIncidentById(String id) {
        Incident incident = findIncidentEntityById(id);
        return mapToDto(incident);
    }

    public Incident findIncidentEntityById(String id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
    }

    public IncidentResponseDto updateIncident(String id, UpdateIncidentRequest request, String actor) {
        Incident incident = findIncidentEntityById(id);

        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setSeverity(request.severity());
        if (request.impact() != null) incident.setImpact(request.impact());
        if (request.rootCause() != null) incident.setRootCause(request.rootCause());
        if (request.resolution() != null) incident.setResolution(request.resolution());
        if (request.tags() != null) incident.setTags(request.tags());
        incident.setUpdatedAt(Instant.now());

        Incident saved = incidentRepository.save(incident);
        log.info("Updated incident {} details", id);
        return mapToDto(saved);
    }

    public IncidentResponseDto assignIncident(String id, String engineerId, String actor) {
        Incident incident = findIncidentEntityById(id);

        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found with id: " + engineerId));

        if (engineer.getRole() != Role.ENGINEER && engineer.getRole() != Role.ADMIN) {
            throw new BadRequestException("Assigned user must hold ENGINEER or ADMIN role");
        }

        incident.setAssignedEngineerId(engineer.getId());
        incident.setUpdatedAt(Instant.now());

        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.ACKNOWLEDGED);
            incident.setAcknowledgedAt(Instant.now());
        }

        Incident saved = incidentRepository.save(incident);

        createTimelineEvent(
                saved.getId(),
                EventType.ENGINEER_ASSIGNED,
                "Incident assigned to " + engineer.getName() + " (" + engineer.getEmail() + ")",
                actor != null ? actor : "SYSTEM",
                "INCIDENT_SERVICE",
                Map.of("engineerId", engineer.getId(), "engineerName", engineer.getName())
        );

        log.info("Assigned incident {} to engineer {}", id, engineer.getName());
        return mapToDto(saved);
    }

    public IncidentResponseDto updateStatus(String id, IncidentStatus newStatus, String note, String actor) {
        Incident incident = findIncidentEntityById(id);
        IncidentStatus oldStatus = incident.getStatus();

        stateMachine.validateTransition(oldStatus, newStatus);

        Instant now = Instant.now();
        incident.setStatus(newStatus);
        incident.setUpdatedAt(now);

        if (newStatus == IncidentStatus.ACKNOWLEDGED && incident.getAcknowledgedAt() == null) {
            incident.setAcknowledgedAt(now);
        } else if (newStatus == IncidentStatus.MITIGATED && incident.getMitigatedAt() == null) {
            incident.setMitigatedAt(now);
        } else if (newStatus == IncidentStatus.RESOLVED && incident.getResolvedAt() == null) {
            incident.setResolvedAt(now);
        } else if (newStatus == IncidentStatus.CLOSED && incident.getClosedAt() == null) {
            incident.setClosedAt(now);
        }

        Incident saved = incidentRepository.save(incident);

        String message = String.format("Status changed from %s to %s", oldStatus, newStatus);
        if (note != null && !note.isBlank()) {
            message += ". Note: " + note;
        }

        createTimelineEvent(
                id,
                EventType.STATUS_CHANGED,
                message,
                actor != null ? actor : "SYSTEM",
                "INCIDENT_SERVICE",
                Map.of("oldStatus", oldStatus.name(), "newStatus", newStatus.name())
        );

        if (newStatus == IncidentStatus.RESOLVED) {
            createTimelineEvent(
                    id,
                    EventType.INCIDENT_RESOLVED,
                    "Incident marked as RESOLVED",
                    actor != null ? actor : "SYSTEM",
                    "INCIDENT_SERVICE",
                    Map.of("resolvedAt", now.toString())
            );
        }

        log.info("Transitioned incident {} status from {} to {}", id, oldStatus, newStatus);
        return mapToDto(saved);
    }

    public IncidentResponseDto addNote(String id, String note, String actor) {
        findIncidentEntityById(id); // verify existence

        createTimelineEvent(
                id,
                EventType.NOTE_ADDED,
                note,
                actor != null ? actor : "SYSTEM",
                "USER",
                Map.of()
        );

        log.info("Added note to incident {} by {}", id, actor);
        return getIncidentById(id);
    }

    public List<IncidentEvent> getIncidentTimeline(String incidentId) {
        findIncidentEntityById(incidentId); // verify exists
        return incidentEventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
    }

    public void deleteIncident(String id) {
        Incident incident = findIncidentEntityById(id);
        incidentEventRepository.deleteByIncidentId(id);
        incidentRepository.delete(incident);
        log.info("Deleted incident {}", id);
    }

    public void createTimelineEvent(
            String incidentId,
            EventType type,
            String message,
            String actor,
            String source,
            Map<String, Object> metadata) {

        IncidentEvent event = IncidentEvent.builder()
                .incidentId(incidentId)
                .type(type)
                .message(message)
                .actor(actor)
                .source(source)
                .timestamp(Instant.now())
                .metadata(metadata != null ? metadata : Map.of())
                .build();

        incidentEventRepository.save(event);
    }

    public IncidentResponseDto mapToDto(Incident incident) {
        String serviceName = "Unknown Service";
        try {
            Service service = serviceRegistryService.findServiceEntityById(incident.getAffectedServiceId());
            serviceName = service.getName();
        } catch (Exception ignored) {
        }

        String engineerName = null;
        if (incident.getAssignedEngineerId() != null) {
            engineerName = userRepository.findById(incident.getAssignedEngineerId())
                    .map(User::getName)
                    .orElse("Unknown Engineer");
        }

        return IncidentResponseDto.from(incident, serviceName, engineerName);
    }
}
