package vn.thefilm.backend.enums;

/**
 * Kết quả của một lần gửi hóa đơn tới nhà cung cấp giả lập.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TaxProviderAttemptStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Lần gửi giả lập được xác nhận đã tiếp nhận. */
    ACCEPTED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Quá thời gian chờ phản hồi; cần đối soát trước khi kết luận. */
    TIMEOUT,

    /** Đã phát sinh lỗi khi xử lý. */
    ERROR;
}
