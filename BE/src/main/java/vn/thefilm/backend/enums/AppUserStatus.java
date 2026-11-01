package vn.thefilm.backend.enums;

/**
 * Trạng thái tài khoản nghiệp vụ liên kết với Keycloak.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum AppUserStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Bị khóa hoặc bị chặn, không được phép sử dụng. */
    BLOCKED,

    /** Thông tin nhận dạng đã được ẩn danh theo chính sách dữ liệu. */
    ANONYMIZED;
}
