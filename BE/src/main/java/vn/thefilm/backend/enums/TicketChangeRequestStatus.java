package vn.thefilm.backend.enums;

/**
 * Trạng thái yêu cầu đổi vé.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TicketChangeRequestStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Giao dịch đang được xử lý. */
    PROCESSING,

    /** Quy trình đã hoàn tất. */
    COMPLETED,

    /** Giao dịch xử lý thất bại. */
    FAILED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
