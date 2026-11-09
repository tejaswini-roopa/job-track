package com.jobtrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 80) String industry,
        @Size(max = 200) String website,
        @Size(max = 100) String location) {
}
