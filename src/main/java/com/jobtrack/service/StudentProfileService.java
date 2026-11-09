package com.jobtrack.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.request.UpdateProfileRequest;
import com.jobtrack.dto.response.ProfileResponse;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.StudentProfileRepository;

@Service
public class StudentProfileService {

    private final StudentProfileRepository profileRepository;

    public StudentProfileService(StudentProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        return EntityMapper.toProfileResponse(find(userId));
    }

    /** No save() call: the entity is managed, so Hibernate dirty checking flushes the changes. */
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        StudentProfile profile = find(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            profile.getUser().setFullName(request.fullName().trim());
        }
        profile.setPhone(request.phone());
        profile.setCollege(request.college());
        profile.setDegree(request.degree());
        profile.setBranch(request.branch());
        profile.setGraduationYear(request.graduationYear());
        profile.setCgpa(request.cgpa());
        profile.setSkills(request.skills());
        profile.setResumeUrl(request.resumeUrl());
        return EntityMapper.toProfileResponse(profile);
    }

    private StudentProfile find(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for this user"));
    }
}
