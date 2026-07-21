package com.example.medicalsearch.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        List<FieldErrorResponse> fieldErrors
) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, List.of());
    }

    public static ErrorResponse of(int status, String error, String message, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, fieldErrors);
    }
}
