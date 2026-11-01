package vn.thefilm.backend.enums;

/**
 * Trạng thái hồ sơ dữ liệu của khách hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum CustomerProfileStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Thông tin nhận dạng đã được ẩn danh theo chính sách dữ liệu. */
    ANONYMIZED;
}
