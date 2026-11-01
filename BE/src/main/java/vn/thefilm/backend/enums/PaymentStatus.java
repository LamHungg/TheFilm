package vn.thefilm.backend.enums;

/**
 * Trạng thái vòng đời giao dịch thanh toán.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PaymentStatus {

    /** Giao dịch đã được khởi tạo. */
    INITIATED,

    /** Giao dịch đang được xử lý. */
    PROCESSING,

    /** Giao dịch đã hoàn tất thành công. */
    SUCCEEDED,

    /** Giao dịch xử lý thất bại. */
    FAILED,

    /** Quá thời gian chờ phản hồi; cần đối soát trước khi kết luận. */
    TIMEOUT,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
