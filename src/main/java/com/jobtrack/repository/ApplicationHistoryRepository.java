package com.jobtrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jobtrack.entity.ApplicationHistory;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, Long> {

    List<ApplicationHistory> findByApplicationIdOrderByChangedAtDesc(Long applicationId);
}
