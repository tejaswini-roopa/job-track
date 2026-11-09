package com.jobtrack.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtrack.dto.request.CompanyRequest;
import com.jobtrack.dto.response.CompanyResponse;
import com.jobtrack.dto.response.PageResponse;
import com.jobtrack.entity.Company;
import com.jobtrack.exception.BusinessRuleException;
import com.jobtrack.exception.DuplicateResourceException;
import com.jobtrack.exception.ResourceNotFoundException;
import com.jobtrack.mapper.EntityMapper;
import com.jobtrack.repository.CompanyRepository;
import com.jobtrack.repository.JobRepository;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;

    public CompanyService(CompanyRepository companyRepository, JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> list(String search, Pageable pageable) {
        Page<Company> page = (search == null || search.isBlank())
                ? companyRepository.findAll(pageable)
                : companyRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        return PageResponse.from(page.map(EntityMapper::toCompanyResponse));
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(Long id) {
        return EntityMapper.toCompanyResponse(find(id));
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("Company already exists: " + request.name());
        }
        Company company = new Company();
        copy(request, company);
        return EntityMapper.toCompanyResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = find(id);
        companyRepository.findByNameIgnoreCase(request.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Company already exists: " + request.name());
                });
        copy(request, company);
        return EntityMapper.toCompanyResponse(company);
    }

    @Transactional
    public void delete(Long id) {
        Company company = find(id);
        if (jobRepository.existsByCompanyId(id)) {
            throw new BusinessRuleException("Cannot delete a company that still has job postings");
        }
        companyRepository.delete(company);
    }

    private Company find(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private void copy(CompanyRequest r, Company c) {
        c.setName(r.name().trim());
        c.setIndustry(r.industry());
        c.setWebsite(r.website());
        c.setLocation(r.location());
    }
}
