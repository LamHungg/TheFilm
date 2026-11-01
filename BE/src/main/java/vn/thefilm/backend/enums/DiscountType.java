package vn.thefilm.backend.enums;

/**
 * Cách tính mức ưu đãi cho chiến dịch khuyến mại.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum DiscountType {

    /** Giá trị được tính theo số tiền cố định. */
    FIXED,

    /** Giá trị được tính theo tỷ lệ phần trăm. */
    PERCENT,

    /** Ưu đãi được quy đổi hoặc thanh toán bằng điểm thành viên. */
    POINTS;
}
