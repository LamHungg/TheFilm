package vn.thefilm.backend.enums;

/**
 * Loại biến động trong sổ điểm thành viên.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PointsEntryType {

    /** Điểm được cộng vào tài khoản thành viên. */
    EARN,

    /** Điểm được dùng để thanh toán hoặc đổi ưu đãi. */
    REDEEM,

    /** Điểm được điều chỉnh thủ công có ghi nhận lý do. */
    ADJUST,

    /** Điểm hết hạn và bị trừ khỏi số dư. */
    EXPIRE;
}
