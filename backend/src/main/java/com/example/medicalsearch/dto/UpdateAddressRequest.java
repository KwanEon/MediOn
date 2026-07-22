package com.example.medicalsearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @NotBlank(message = "주소를 입력해 주세요.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address
) {
}
