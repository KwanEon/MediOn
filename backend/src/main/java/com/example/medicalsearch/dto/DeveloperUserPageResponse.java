package com.example.medicalsearch.dto;

import java.util.List;

public record DeveloperUserPageResponse(
        List<DeveloperUserResponse> items,
        PageResponse page
) {
}
