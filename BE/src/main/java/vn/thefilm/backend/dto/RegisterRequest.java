package vn.thefilm.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập địa chỉ email.")
        @Email(message = "Địa chỉ email không đúng định dạng.")
        @Size(max = 320, message = "Email không được vượt quá 320 ký tự.")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu.")
        @Size(min = 6, max = 100, message = "Mật khẩu phải có từ 6 đến 100 ký tự.")
        String password,

        @NotBlank(message = "Vui lòng nhập họ và tên.")
        @Size(min = 2, max = 200, message = "Họ và tên phải có từ 2 đến 200 ký tự.")
        String displayName,

        @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
        String phone
) { }

