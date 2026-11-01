package vn.thefilm.backend.enums;

/**
 * Trạng thái tài khoản thành viên và quyền tích điểm.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum MemberStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Tạm khóa quyền sử dụng; có thể được mở lại. */
    SUSPENDED,

    /** Đã đóng; không nhận nghiệp vụ mới. */
    CLOSED;
}
