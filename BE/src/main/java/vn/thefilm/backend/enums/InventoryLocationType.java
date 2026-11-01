package vn.thefilm.backend.enums;

/**
 * Loại địa điểm quản lý hàng tồn kho.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum InventoryLocationType {

    /** Kho lưu trữ hàng hóa. */
    WAREHOUSE,

    /** Quầy bán hàng hoặc khu vực phục vụ tại rạp. */
    CONCESSION,

    /** Nhóm khác, dùng khi không thuộc các loại đã định nghĩa. */
    OTHER;
}
