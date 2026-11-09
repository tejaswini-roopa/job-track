package com.jobtrack.mapper;

import com.jobtrack.dto.response.ApplicationResponse;
import com.jobtrack.dto.response.CompanyResponse;
import com.jobtrack.dto.response.HistoryResponse;
import com.jobtrack.dto.response.InterviewResponse;
import com.jobtrack.dto.response.JobResponse;
import com.jobtrack.dto.response.ProfileResponse;
import com.jobtrack.dto.response.SavedJobResponse;
import com.jobtrack.dto.response.UserResponse;
import com.jobtrack.entity.ApplicationHistory;
import com.jobtrack.entity.Company;
import com.jobtrack.entity.Interview;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.JobApplication;
import com.jobtrack.entity.SavedJob;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.entity.User;

/**
 * Entity -> DTO conversion. Must be called inside a transaction because it
 * touches lazy associations (open-in-view is disabled).
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                u.isEnabled(), u.getCreatedAt());
    }

    public static ProfileResponse toProfileResponse(StudentProfile p) {
        User u = p.getUser();
        return new ProfileResponse(p.getId(), u.getId(), u.getEmail(), u.getFullName(), p.getPhone(),
                p.getCollege(), p.getDegree(), p.getBranch(), p.getGraduationYear(),
                p.getCgpa(), p.getSkills(), p.getResumeUrl());
    }

    public static CompanyResponse toCompanyResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getName(), c.getIndustry(), c.getWebsite(), c.getLocation());
    }

    public static JobResponse toJobResponse(Job j) {
        Company c = j.getCompany();
        return new JobResponse(j.getId(), c.getId(), c.getName(), j.getTitle(), j.getDescription(),
                j.getLocation(), j.getJobType(), j.getSkillsRequired(), j.getMinCgpa(),
                j.getSalaryLpa(), j.getApplicationDeadline(), j.getDriveDate(),
                j.getStatus(), j.getCreatedAt());
    }

    public static ApplicationResponse toApplicationResponse(JobApplication a) {
        Job j = a.getJob();
        StudentProfile s = a.getStudent();
        User u = s.getUser();
        return new ApplicationResponse(a.getId(), s.getId(), u.getFullName(), u.getEmail(),
                j.getId(), j.getTitle(), j.getCompany().getName(),
                a.getStatus(), a.getNotes(), a.getAppliedAt(), a.getUpdatedAt());
    }

    public static InterviewResponse toInterviewResponse(Interview i) {
        JobApplication a = i.getApplication();
        Job j = a.getJob();
        return new InterviewResponse(i.getId(), a.getId(), j.getTitle(), j.getCompany().getName(),
                i.getRoundName(), i.getScheduledAt(), i.getMode(), i.getLocationOrLink());
    }

    public static SavedJobResponse toSavedJobResponse(SavedJob s) {
        return new SavedJobResponse(s.getId(), toJobResponse(s.getJob()), s.getSavedAt());
    }

    public static HistoryResponse toHistoryResponse(ApplicationHistory h) {
        String changedBy = h.getChangedBy() == null ? null : h.getChangedBy().getFullName();
        return new HistoryResponse(h.getId(), h.getOldStatus(), h.getNewStatus(),
                changedBy, h.getRemarks(), h.getChangedAt());
    }
}
