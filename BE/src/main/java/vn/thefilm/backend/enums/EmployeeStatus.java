package vn.thefilm.backend.enums;

/**
 * Trạng thái làm việc của hồ sơ nhân viên.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum EmployeeStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Nhân viên đang trong thời gian nghỉ đã được ghi nhận. */
    ON_LEAVE,

    /** Quan hệ lao động hoặc hợp đồng đã chấm dứt. */
    TERMINATED;
}
