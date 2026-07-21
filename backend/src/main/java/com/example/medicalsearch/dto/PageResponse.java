package com.example.medicalsearch.dto;

public record PageResponse(
        int number,
        int size,
        long totalElements,
        int totalPages
) {

    public static PageResponse of(int number, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse(number, size, totalElements, totalPages);
    }
}
