package com.example.bookingsystem.controller;

import com.example.bookingsystem.dto.resource.ResourceCreateRequest;
import com.example.bookingsystem.dto.resource.ResourceResponse;
import com.example.bookingsystem.dto.resource.ResourceUpdateRequest;
import com.example.bookingsystem.service.ResourceService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceCreateRequest request
    ) {

        ResourceResponse response =
                resourceService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ResourceResponse>> getAll(
            @ParameterObject
            @PageableDefault(
                    size = 10,
                    sort = "name"
            )
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                resourceService.getAll(pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                resourceService.getById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceUpdateRequest request
    ) {

        return ResponseEntity.ok(
                resourceService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        resourceService.delete(id);

        return ResponseEntity.noContent().build();
    }
}