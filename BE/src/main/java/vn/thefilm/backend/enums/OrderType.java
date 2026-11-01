package vn.thefilm.backend.enums;

/**
 * Nhóm nghiệp vụ chính của một đơn hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum OrderType {

    /** Đơn chỉ bao gồm vé xem phim. */
    TICKET,

    /** Đơn chỉ bao gồm đồ ăn, thức uống. */
    FNB,

    /** Đơn kết hợp vé xem phim và đồ ăn, thức uống. */
    COMBINED;
}
