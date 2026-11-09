package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.response.SavedJobResponse;
import com.jobtrack.entity.Company;
import com.jobtrack.entity.Job;
import com.jobtrack.entity.SavedJob;
import com.jobtrack.entity.StudentProfile;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.repository.JobRepository;
import com.jobtrack.repository.SavedJobRepository;
import com.jobtrack.repository.StudentProfileRepository;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository savedJobRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @InjectMocks
    private SavedJobService service;

    private StudentProfile student;
    private Job job;

    @BeforeEach
    void setUp() {
        student = new StudentProfile();
        student.setId(1L);
        Company company = new Company();
        company.setId(2L);
        company.setName("Acme");
        job = new Job();
        job.setId(5L);
        job.setTitle("Java Developer");
        job.setCompany(company);
    }

    @Test
    void saveRejectsDuplicate() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByStudentIdAndJobId(1L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.save(10L, 5L)).isInstanceOf(DuplicateResourceException.class);
        verify(savedJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveStoresJobInWishlist() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(savedJobRepository.existsByStudentIdAndJobId(1L, 5L)).thenReturn(false);
        when(savedJobRepository.saveAndFlush(any(SavedJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SavedJobResponse response = service.save(10L, 5L);

        assertThat(response.job().title()).isEqualTo("Java Developer");
        assertThat(response.job().companyName()).isEqualTo("Acme");
    }

    @Test
    void removeThrowsWhenJobIsNotInWishlist() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(savedJobRepository.findByStudentIdAndJobId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(10L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
