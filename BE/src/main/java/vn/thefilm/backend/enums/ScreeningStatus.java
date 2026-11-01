package vn.thefilm.backend.enums;

/**
 * Trạng thái lập lịch và mở bán của suất chiếu.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ScreeningStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đã lên lịch chiếu nhưng chưa mở bán. */
    SCHEDULED,

    /** Đang mở bán vé. */
    ON_SALE,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED,

    /** Quy trình đã hoàn tất. */
    COMPLETED;
}
