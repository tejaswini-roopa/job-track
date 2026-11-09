package com.jobtrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtrack.entity.StudentProfile;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);
}
