package com.example.bookingsystem.repository;

import com.example.bookingsystem.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("""
            UPDATE User u
            SET u.tokenVersion = u.tokenVersion + 1
            WHERE u.email = :email
            """)
    int incrementTokenVersionByEmail(@Param("email") String email);
}