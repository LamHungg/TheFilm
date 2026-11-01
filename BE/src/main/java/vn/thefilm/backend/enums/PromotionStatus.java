package vn.thefilm.backend.enums;

/**
 * Trạng thái hiệu lực của chiến dịch khuyến mại.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PromotionStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Tạm dừng áp dụng; có thể được kích hoạt lại. */
    PAUSED,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
