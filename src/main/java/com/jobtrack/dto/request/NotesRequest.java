package com.jobtrack.dto.request;

import jakarta.validation.constraints.Size;

public record NotesRequest(@Size(max = 5000) String notes) {
}
