package vn.thefilm.backend.enums;

/**
 * Trạng thái gửi sự kiện tích hợp sang hệ thống ngoài.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum OutboxEventStatus {

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Giao dịch đang được xử lý. */
    PROCESSING,

    /** Callback đã được xử lý thành công. */
    PROCESSED,

    /** Đang chờ được gửi lại theo chính sách thử lại. */
    RETRY,

    /** Đã hết số lần thử lại tự động, cần xử lý thủ công. */
    DEAD;
}
