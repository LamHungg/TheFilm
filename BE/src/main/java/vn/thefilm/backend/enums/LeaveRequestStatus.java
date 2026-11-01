package vn.thefilm.backend.enums;

/**
 * Trạng thái xử lý đơn xin nghỉ của nhân viên.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum LeaveRequestStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
