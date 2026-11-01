package vn.thefilm.backend.enums;

/**
 * Trạng thái ghi sổ của giao dịch tồn kho.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum InventoryTransactionStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Giao dịch tồn kho đã ghi sổ. */
    POSTED,

    /** Giao dịch tồn kho đã được đảo bằng giao dịch đối ứng. */
    REVERSED;
}
