package com.example.medicalsearch.dto;

public record FieldErrorResponse(
        String field,
        String message
) {
}
