package com.example.medicalsearch.dto;

import java.util.List;

public record DeveloperInquiryPageResponse(
        List<DeveloperInquiryResponse> items,
        PageResponse page
) {
}
