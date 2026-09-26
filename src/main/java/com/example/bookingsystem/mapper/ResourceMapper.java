package com.example.bookingsystem.mapper;

import com.example.bookingsystem.dto.resource.ResourceCreateRequest;
import com.example.bookingsystem.dto.resource.ResourceResponse;
import com.example.bookingsystem.dto.resource.ResourceUpdateRequest;
import com.example.bookingsystem.entity.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public Resource toEntity(ResourceCreateRequest request) {
        return new Resource(
                request.name(),
                request.description(),
                request.price(),
                request.available()
        );
    }

    public void updateEntity(Resource resource, ResourceUpdateRequest request) {
        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setPrice(request.price());
        resource.setAvailable(request.available());
    }

    public ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getPrice(),
                resource.isAvailable()
        );
    }
}