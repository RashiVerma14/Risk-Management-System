package com.riskmanager.serviceregistry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDependencyDto {

    private ServiceResponseDto service;
    private List<ServiceResponseDto> upstreamDependencies;   // Services this service calls
    private List<ServiceResponseDto> downstreamDependents;   // Services calling this service
}
