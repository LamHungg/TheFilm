package vn.thefilm.backend.enums;

/**
 * Trường chấm công mà nhân viên yêu cầu điều chỉnh.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum AttendanceChangeField {

    /** Giờ vào ca. */
    CLOCK_IN,

    /** Giờ kết thúc ca. */
    CLOCK_OUT,

    /** Trường trạng thái chấm công cần hiệu chỉnh. */
    STATUS;
}
