package vn.thefilm.backend.enums;

/**
 * Trạng thái ghế được cấp cho một suất chiếu.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ShowSeatStatus {

    /** Ghế đang trống và có thể được giữ hoặc bán. */
    AVAILABLE,

    /** Ghế đang được giữ tạm thời cho một đơn chưa thanh toán. */
    HELD,

    /** Ghế đã bán và gắn với vé. */
    SOLD,

    /** Bị khóa hoặc bị chặn, không được phép sử dụng. */
    BLOCKED;
}
