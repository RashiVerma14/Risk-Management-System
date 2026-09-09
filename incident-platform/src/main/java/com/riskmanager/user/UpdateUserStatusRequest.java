package com.riskmanager.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Active status must be specified")
        Boolean active
) {}
