package com.example.medicalsearch.dto;

public record SyncTriggerResponse(
        boolean accepted,
        String target,
        String message
) {
}
