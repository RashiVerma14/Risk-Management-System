package com.riskmanager.incident;

import com.riskmanager.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class IncidentStateMachine {

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(IncidentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(IncidentStatus.OPEN, EnumSet.of(
                IncidentStatus.ACKNOWLEDGED,
                IncidentStatus.INVESTIGATING,
                IncidentStatus.MITIGATED,
                IncidentStatus.RESOLVED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.ACKNOWLEDGED, EnumSet.of(
                IncidentStatus.INVESTIGATING,
                IncidentStatus.MITIGATED,
                IncidentStatus.RESOLVED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.INVESTIGATING, EnumSet.of(
                IncidentStatus.MITIGATED,
                IncidentStatus.RESOLVED
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.MITIGATED, EnumSet.of(
                IncidentStatus.RESOLVED,
                IncidentStatus.INVESTIGATING
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.RESOLVED, EnumSet.of(
                IncidentStatus.CLOSED,
                IncidentStatus.INVESTIGATING // Re-opened if issue recurs
        ));

        ALLOWED_TRANSITIONS.put(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));
    }

    public boolean isValidTransition(IncidentStatus current, IncidentStatus target) {
        if (current == target) {
            return true;
        }
        Set<IncidentStatus> validTargets = ALLOWED_TRANSITIONS.get(current);
        return validTargets != null && validTargets.contains(target);
    }

    public void validateTransition(IncidentStatus current, IncidentStatus target) {
        if (!isValidTransition(current, target)) {
            throw new BadRequestException(
                    String.format("Invalid incident status transition: cannot transition from %s to %s", current, target)
            );
        }
    }
}
