package vn.thefilm.backend.enums;

/**
 * Trạng thái bàn giao và xác nhận ca.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ShiftHandoverStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED;
}
