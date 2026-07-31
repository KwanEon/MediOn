package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.AppUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeveloperUserResponse(
        Long id,
        String username,
        String role,
        String name,
        String email,
        String phoneNumber,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        long favoriteCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DeveloperUserResponse from(AppUser user, long favoriteCount) {
        return new DeveloperUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getLatitude(),
                user.getLongitude(),
                favoriteCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
