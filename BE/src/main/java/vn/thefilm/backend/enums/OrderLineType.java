package vn.thefilm.backend.enums;

/**
 * Nhóm mặt hàng của từng dòng trong đơn bán.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum OrderLineType {

    /** Dòng đơn ghi nhận một vé xem phim. */
    TICKET,

    /** Dòng đơn ghi nhận sản phẩm đồ ăn, thức uống. */
    FNB;
}
