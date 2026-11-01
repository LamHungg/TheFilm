package vn.thefilm.backend.enums;

/**
 * Loại biến động làm tăng, giảm hoặc điều chỉnh tồn kho.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum InventoryTransactionType {

    /** Nhập hàng làm tăng tồn kho. */
    RECEIPT,

    /** Xuất hàng làm giảm tồn kho. */
    ISSUE,

    /** Nhận hàng chuyển từ địa điểm khác. */
    TRANSFER_IN,

    /** Chuyển hàng sang địa điểm khác. */
    TRANSFER_OUT,

    /** Ghi nhận kết quả kiểm kê thực tế. */
    COUNT,

    /** Ghi nhận hàng hỏng hoặc thất thoát. */
    WASTE,

    /** Giao dịch hoặc hóa đơn điều chỉnh số liệu đã ghi nhận. */
    ADJUSTMENT,

    /** Xuất kho do bán hàng. */
    SALE;
}
