package vn.thefilm.backend.enums;

/**
 * Trạng thái vòng đời đơn hàng bán vé hoặc F&B.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum OrderStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đơn đang chờ hoàn tất thanh toán. */
    PENDING_PAYMENT,

    /** Đơn đã được thanh toán thành công. */
    PAID,

    /** Vé đã được phát hành cho khách. */
    ISSUED,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED,

    /** Đơn đang chờ xử lý hoàn tiền. */
    REFUND_PENDING,

    /** Một phần giá trị đơn đã được hoàn. */
    PARTIALLY_REFUNDED,

    /** Đơn đã được hoàn toàn bộ giá trị đủ điều kiện. */
    REFUNDED;
}
