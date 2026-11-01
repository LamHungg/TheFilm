package vn.thefilm.backend.enums;

/**
 * Trạng thái kỳ tính lương.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PayrollPeriodStatus {

    /** Đang mở để tiếp tục ghi nhận hoặc xử lý. */
    OPEN,

    /** Giao dịch đang được xử lý. */
    PROCESSING,

    /** Đã đóng; không nhận nghiệp vụ mới. */
    CLOSED;
}
