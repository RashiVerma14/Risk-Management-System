package com.riskmanager.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role must be specified")
        Role role
) {}
