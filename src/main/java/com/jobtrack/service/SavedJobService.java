package com.jobtrack.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.dto.response.SavedJobResponse;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.SavedJob;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.SavedJobRepository;
import com.jobtrack.repository.StudentProfileRepository;

@Service
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final StudentProfileRepository profileRepository;

    public SavedJobService(SavedJobRepository savedJobRepository,
                           JobRepository jobRepository,
                           StudentProfileRepository profileRepository) {
        this.savedJobRepository = savedJobRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public SavedJobResponse save(Long userId, Long jobId) {
        StudentProfile student = findProfile(userId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        if (savedJobRepository.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw new DuplicateResourceException("Job is already in your wishlist");
        }
        SavedJob saved = new SavedJob();
        saved.setStudent(student);
        saved.setJob(job);
        try {
            return EntityMapper.toSavedJobResponse(savedJobRepository.saveAndFlush(saved));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Job is already in your wishlist");
        }
    }

    @Transactional
    public void remove(Long userId, Long jobId) {
        StudentProfile student = findProfile(userId);
        SavedJob saved = savedJobRepository.findByStudentIdAndJobId(student.getId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job is not in your wishlist"));
        savedJobRepository.delete(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<SavedJobResponse> list(Long userId, Pageable pageable) {
        StudentProfile student = findProfile(userId);
        return PageResponse.from(savedJobRepository.findByStudentId(student.getId(), pageable)
                .map(EntityMapper::toSavedJobResponse));
    }

    private StudentProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this user"));
    }
}
