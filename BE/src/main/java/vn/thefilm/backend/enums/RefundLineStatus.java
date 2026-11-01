package vn.thefilm.backend.enums;

/**
 * Trạng thái xử lý hoàn tiền của từng dòng đơn.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum RefundLineStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Giao dịch đang được xử lý. */
    PROCESSING,

    /** Giao dịch đã hoàn tất thành công. */
    SUCCEEDED,

    /** Giao dịch xử lý thất bại. */
    FAILED;
}
