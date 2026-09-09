package com.riskmanager.incident;

import com.riskmanager.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentStateMachineTest {

    private IncidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new IncidentStateMachine();
    }

    @Test
    void testValidTransitions() {
        // OPEN transitions
        assertTrue(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.MITIGATED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.RESOLVED));

        // ACKNOWLEDGED transitions
        assertTrue(stateMachine.isValidTransition(IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.ACKNOWLEDGED, IncidentStatus.RESOLVED));

        // INVESTIGATING transitions
        assertTrue(stateMachine.isValidTransition(IncidentStatus.INVESTIGATING, IncidentStatus.MITIGATED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.INVESTIGATING, IncidentStatus.RESOLVED));

        // RESOLVED transitions
        assertTrue(stateMachine.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.CLOSED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.INVESTIGATING)); // Reopened
    }

    @Test
    void testInvalidTransitionsThrowBadRequestException() {
        // Cannot transition directly from CLOSED to INVESTIGATING
        assertFalse(stateMachine.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.INVESTIGATING));
        assertThrows(BadRequestException.class, () ->
                stateMachine.validateTransition(IncidentStatus.CLOSED, IncidentStatus.INVESTIGATING));

        // Cannot transition from CLOSED to OPEN
        assertFalse(stateMachine.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.OPEN));
        assertThrows(BadRequestException.class, () ->
                stateMachine.validateTransition(IncidentStatus.CLOSED, IncidentStatus.OPEN));
    }
}
