package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.Inquiry;
import java.time.LocalDateTime;

public record DeveloperInquiryResponse(
        Long id,
        String category,
        String title,
        String content,
        String status,
        LocalDateTime createdAt,
        Long userId,
        String username,
        String userName,
        String email,
        String phoneNumber
) {

    public static DeveloperInquiryResponse from(Inquiry inquiry, AppUser user) {
        return new DeveloperInquiryResponse(
                inquiry.getId(),
                inquiry.getCategory().name(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt(),
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }
}
