package vn.thefilm.backend.enums;

/**
 * Phương pháp tính số tiền thuế trong môi trường giả lập.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TaxCalculationMethod {

    /** Thuế được cộng thêm ngoài giá chưa thuế. */
    EXCLUSIVE,

    /** Giá đã bao gồm thuế; hệ thống tách phần thuế. */
    INCLUSIVE,

    /** Tính theo các bậc thuế lũy tiến. */
    PROGRESSIVE,

    /** Tính thuế theo một số tiền cố định. */
    FIXED;
}
