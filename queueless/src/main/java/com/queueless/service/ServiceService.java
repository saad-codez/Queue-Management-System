
package com.queueless.service;

import com.queueless.entity.Service;
import com.queueless.repository.ServiceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public Service createService(String name) {
        Service service = new Service(name);
        return serviceRepository.save(service);
    }

    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Service getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
    }

    @Transactional
    public Service getServiceForUpdate(Long id) {
        return serviceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
    }


    public Service updateServiceStatus(Long id, boolean active) {

        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        service.setActive(active);

        return serviceRepository.save(service);
    }

}
