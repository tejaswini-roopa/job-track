package com.jobtrack.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtrack.dto.request.CompanyRequest;
import com.jobtrack.entity.Company;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.repository.CompanyRepository;
import com.jobtrack.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private JobRepository jobRepository;
    @InjectMocks
    private CompanyService service;

    private Company company(long id, String name) {
        Company c = new Company();
        c.setId(id);
        c.setName(name);
        return c;
    }

    @Test
    void createRejectsDuplicateName() {
        when(companyRepository.existsByNameIgnoreCase("Acme")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CompanyRequest("Acme", null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(companyRepository, never()).save(any());
    }

    @Test
    void deleteIsBlockedWhileJobsExist() {
        Company c = company(1L, "Acme");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(c));
        when(jobRepository.existsByCompanyId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessRuleException.class);
        verify(companyRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesCompanyWithoutJobs() {
        Company c = company(1L, "Acme");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(c));
        when(jobRepository.existsByCompanyId(1L)).thenReturn(false);

        service.delete(1L);

        verify(companyRepository).delete(c);
    }

    @Test
    void updateRejectsNameOwnedByAnotherCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company(1L, "Acme")));
        when(companyRepository.findByNameIgnoreCase("Globex")).thenReturn(Optional.of(company(2L, "Globex")));

        assertThatThrownBy(() -> service.update(1L, new CompanyRequest("Globex", null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
