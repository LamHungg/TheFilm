package vn.thefilm.backend.enums;

/**
 * Trạng thái sử dụng của mã voucher.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum VoucherStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Voucher đã được dùng và không thể áp dụng lại. */
    REDEEMED,

    /** Đã hết thời hạn hoặc hiệu lực. */
    EXPIRED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
