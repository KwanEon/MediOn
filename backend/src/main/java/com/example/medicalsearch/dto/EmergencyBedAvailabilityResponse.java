package com.example.medicalsearch.dto;

import java.util.Map;

public record EmergencyBedAvailabilityResponse(
        Map<Long, Integer> availableBeds
) {
}
