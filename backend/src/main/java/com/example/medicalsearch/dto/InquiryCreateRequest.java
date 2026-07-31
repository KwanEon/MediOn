package com.example.medicalsearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
        @NotBlank(message = "문의 유형을 선택해 주세요.")
        String category,

        @NotBlank(message = "문의 제목을 입력해 주세요.")
        @Size(max = 150, message = "문의 제목은 150자 이하여야 합니다.")
        String title,

        @NotBlank(message = "문의 내용을 입력해 주세요.")
        @Size(max = 10000, message = "문의 내용은 10,000자 이하여야 합니다.")
        String content
) {
}
