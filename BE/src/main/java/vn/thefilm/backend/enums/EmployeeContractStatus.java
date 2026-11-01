package vn.thefilm.backend.enums;

/**
 * Trạng thái hợp đồng lao động.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum EmployeeContractStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED,

    /** Quan hệ lao động hoặc hợp đồng đã chấm dứt. */
    TERMINATED;
}
