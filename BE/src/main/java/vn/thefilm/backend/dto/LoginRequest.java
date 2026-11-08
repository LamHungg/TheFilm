package vn.thefilm.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập email hoặc tài khoản.")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu.")
        String password
) { }

