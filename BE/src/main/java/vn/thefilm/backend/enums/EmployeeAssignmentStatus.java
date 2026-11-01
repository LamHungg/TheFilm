package vn.thefilm.backend.enums;

/**
 * Trạng thái hiệu lực của lần phân công nhân viên vào rạp.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum EmployeeAssignmentStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Phân công tại rạp đã kết thúc. */
    ENDED;
}
