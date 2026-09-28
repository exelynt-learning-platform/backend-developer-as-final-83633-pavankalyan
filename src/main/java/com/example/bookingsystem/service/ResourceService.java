package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.resource.ResourceCreateRequest;
import com.example.bookingsystem.dto.resource.ResourceResponse;
import com.example.bookingsystem.dto.resource.ResourceUpdateRequest;
import com.example.bookingsystem.entity.Resource;
import com.example.bookingsystem.exception.ResourceInUseException;
import com.example.bookingsystem.exception.ResourceNotFoundException;
import com.example.bookingsystem.mapper.ResourceMapper;
import com.example.bookingsystem.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bookingsystem.repository.ReservationRepository;

@Service
@Transactional
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    private final ReservationRepository reservationRepository;

    public ResourceService(
            ResourceRepository resourceRepository,
            ResourceMapper resourceMapper,
            ReservationRepository reservationRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
        this.reservationRepository = reservationRepository;
    }

    public ResourceResponse create(ResourceCreateRequest request) {

        Resource resource = resourceMapper.toEntity(request);

        Resource savedResource = resourceRepository.save(resource);

        return resourceMapper.toResponse(savedResource);
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAll(Pageable pageable) {

        return resourceRepository.findAll(pageable)
                .map(resourceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {

        Resource resource = findResource(id);

        return resourceMapper.toResponse(resource);
    }

    public ResourceResponse update(
            Long id,
            ResourceUpdateRequest request
    ) {

        Resource resource = findResource(id);

        resourceMapper.updateEntity(resource, request);

        Resource updatedResource = resourceRepository.save(resource);

        return resourceMapper.toResponse(updatedResource);
    }

    public void delete(Long id) {

        Resource resource = findResource(id);

        if (reservationRepository.existsByResourceId(id)) {
            throw new ResourceInUseException(
                    "Resource cannot be deleted because it has existing reservations"
            );
        }

        resourceRepository.delete(resource);
    }

    private Resource findResource(Long id) {

        return resourceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with id: " + id
                        )
                );
    }
}