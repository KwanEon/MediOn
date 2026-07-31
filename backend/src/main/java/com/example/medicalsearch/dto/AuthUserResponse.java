package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.AppUser;
import java.math.BigDecimal;

public record AuthUserResponse(
        Long id,
        String username,
        String name,
        String email,
        String phoneNumber,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String role
) {
    public static AuthUserResponse from(AppUser user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getLatitude(),
                user.getLongitude(),
                user.getRole().name()
        );
    }
}
