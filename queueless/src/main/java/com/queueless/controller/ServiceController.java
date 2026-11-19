
package com.queueless.controller;

import com.queueless.dto.CreateServiceRequest;
import com.queueless.dto.ServiceResponse;
import com.queueless.entity.Service;
import com.queueless.service.ServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(
            @Valid @RequestBody CreateServiceRequest request) {

        return toResponse(serviceService.createService(request.name()));
    }

    @GetMapping
    public List<ServiceResponse> getAllServices() {
        return serviceService.getAllServices()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ServiceResponse getServiceById(@PathVariable Long id) {
        return toResponse(serviceService.getServiceById(id));
    }

    @PatchMapping("/{id}/status")
    public ServiceResponse updateServiceStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        return toResponse(serviceService.updateServiceStatus(id, active));
    }

    private ServiceResponse toResponse(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.isActive()
        );
    }
}
