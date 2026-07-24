package com.example.medicalsearch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EmergencyBedAvailabilityRequest(
        @NotEmpty
        @Size(max = 500)
        List<@Positive Long> institutionIds
) {
}
