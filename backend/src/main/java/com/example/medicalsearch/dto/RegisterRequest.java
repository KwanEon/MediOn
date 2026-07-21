package com.example.medicalsearch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(min = 4, max = 30, message = "아이디는 4자 이상 30자 이하여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "아이디는 영문, 숫자, 밑줄만 사용할 수 있습니다.")
        String username,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
        @Size(max = 150, message = "이메일은 150자 이하여야 합니다.")
        String email,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "^[0-9+() -]{8,30}$", message = "올바른 전화번호를 입력해 주세요.")
        String phoneNumber,

        @NotBlank(message = "주소를 입력해 주세요.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        String address
) {
}
