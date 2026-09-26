package com.example.bookingsystem.repository;

import com.example.bookingsystem.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}