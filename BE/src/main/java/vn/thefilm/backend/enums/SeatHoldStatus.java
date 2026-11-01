package vn.thefilm.backend.enums;

/**
 * Trạng thái giữ ghế tạm thời cho một đơn.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum SeatHoldStatus {

    /** Ghế đang được giữ tạm thời cho một đơn chưa thanh toán. */
    HELD,

    /** Việc giữ ghế hoặc ca làm đã được xác nhận. */
    CONFIRMED,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED,

    /** Ghế đã được giải phóng khỏi đơn giữ. */
    RELEASED;
}
