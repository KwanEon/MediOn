package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.Inquiry;
import java.time.LocalDateTime;

public record InquiryResponse(
        Long id,
        String category,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getCategory().name(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }
}
