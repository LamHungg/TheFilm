package vn.thefilm.backend.enums;

/**
 * Trạng thái áp dụng hoặc đảo giao dịch sử dụng khuyến mại.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum RedemptionStatus {

    /** Ưu đãi đã được áp dụng vào đơn. */
    APPLIED,

    /** Giao dịch trước đó đã được đảo bằng bút toán đối ứng. */
    REVERSED;
}
