package vn.thefilm.backend.enums;

/**
 * Trạng thái sử dụng của vé đã phát hành.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TicketStatus {

    /** Vé còn hiệu lực và có thể được soát. */
    VALID,

    /** Vé đã được quét sử dụng. */
    USED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED,

    /** Đơn đã được hoàn toàn bộ giá trị đủ điều kiện. */
    REFUNDED,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED;
}
