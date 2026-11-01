package vn.thefilm.backend.enums;

/**
 * Trạng thái xử lý sự cố hoặc bảo trì ghế.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum SeatMaintenanceStatus {

    /** Đang mở để tiếp tục ghi nhận hoặc xử lý. */
    OPEN,

    /** Đã có hướng xử lý và được đánh dấu giải quyết. */
    RESOLVED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
