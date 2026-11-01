package vn.thefilm.backend.enums;

/**
 * Trạng thái ghi nhận và duyệt công của nhân viên.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum AttendanceStatus {

    /** Bản ghi công đang mở, chưa gửi duyệt. */
    OPEN,

    /** Đã gửi để chờ người có quyền xem xét. */
    SUBMITTED,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Bản ghi công đã được sửa theo yêu cầu được duyệt. */
    CORRECTED;
}
