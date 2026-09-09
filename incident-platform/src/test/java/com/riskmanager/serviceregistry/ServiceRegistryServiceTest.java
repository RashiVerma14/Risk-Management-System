package com.riskmanager.serviceregistry;

import com.riskmanager.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceRegistryService serviceRegistryService;

    private Service paymentService;
    private Service authService;
    private Service orderService;

    @BeforeEach
    void setUp() {
        authService = Service.builder()
                .id("srv-auth")
                .name("auth-service")
                .ownerTeam("Security")
                .status(ServiceStatus.HEALTHY)
                .build();

        paymentService = Service.builder()
                .id("srv-payment")
                .name("payment-service")
                .ownerTeam("Billing")
                .status(ServiceStatus.HEALTHY)
                .dependencies(List.of("srv-auth"))
                .build();

        orderService = Service.builder()
                .id("srv-order")
                .name("order-service")
                .ownerTeam("Commerce")
                .status(ServiceStatus.HEALTHY)
                .dependencies(List.of("srv-payment"))
                .build();
    }

    @Test
    void testCreateServiceSuccess() {
        CreateServiceRequest req = new CreateServiceRequest(
                "cart-service", "Shopping Cart", "Commerce", "production",
                "http://localhost:8080/health", List.of(), "http://git/cart", "1.0.0"
        );

        when(serviceRepository.existsByName("cart-service")).thenReturn(false);
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> {
            Service s = inv.getArgument(0);
            s.setId("srv-cart");
            return s;
        });

        ServiceResponseDto resp = serviceRegistryService.createService(req);

        assertNotNull(resp);
        assertEquals("cart-service", resp.getName());
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void testCreateDuplicateServiceNameThrowsConflict() {
        CreateServiceRequest req = new CreateServiceRequest(
                "payment-service", "Duplicate", "Billing", "production",
                null, List.of(), null, null
        );

        when(serviceRepository.existsByName("payment-service")).thenReturn(true);

        assertThrows(ConflictException.class, () -> serviceRegistryService.createService(req));
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void testGetDependenciesTraversal() {
        when(serviceRepository.findById("srv-payment")).thenReturn(Optional.of(paymentService));
        when(serviceRepository.findById("srv-auth")).thenReturn(Optional.of(authService));
        when(serviceRepository.findAll()).thenReturn(List.of(authService, paymentService, orderService));

        ServiceDependencyDto graph = serviceRegistryService.getServiceDependencies("srv-payment");

        assertNotNull(graph);
        assertEquals("payment-service", graph.getService().getName());

        // Upstream dependencies (payment depends on auth)
        assertEquals(1, graph.getUpstreamDependencies().size());
        assertEquals("auth-service", graph.getUpstreamDependencies().get(0).getName());

        // Downstream dependents (order depends on payment)
        assertEquals(1, graph.getDownstreamDependents().size());
        assertEquals("order-service", graph.getDownstreamDependents().get(0).getName());
    }
}
