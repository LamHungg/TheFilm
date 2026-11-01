package vn.thefilm.backend.enums;

/**
 * Trạng thái lập, duyệt và chi trả một đợt lương.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PayrollRunStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đợt lương đang chờ bộ phận có thẩm quyền rà soát. */
    REVIEW,

    /** Đợt lương đã được phê duyệt chi trả. */
    APPROVED,

    /** Đợt lương đã được chi trả. */
    PAID,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
