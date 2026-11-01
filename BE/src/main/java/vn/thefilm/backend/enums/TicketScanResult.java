package vn.thefilm.backend.enums;

/**
 * Kết quả kiểm tra QR vé tại cổng soát vé.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TicketScanResult {

    /** QR hợp lệ, vé được ghi nhận vào rạp. */
    ACCEPTED,

    /** Vé đã được quét trước đó nên không cho vào lần nữa. */
    ALREADY_USED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED,

    /** Mã vé không hợp lệ hoặc không thể xác minh. */
    INVALID,

    /** Vé không thuộc rạp đang quét. */
    WRONG_CINEMA;
}
