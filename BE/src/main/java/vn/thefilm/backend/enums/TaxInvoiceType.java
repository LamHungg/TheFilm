package vn.thefilm.backend.enums;

/**
 * Loại hóa đơn giả lập.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TaxInvoiceType {

    /** Hóa đơn bán hàng phát sinh từ giao dịch. */
    SALES,

    /** Giao dịch hoặc hóa đơn điều chỉnh số liệu đã ghi nhận. */
    ADJUSTMENT,

    /** Hóa đơn thay thế hóa đơn trước theo quy trình. */
    REPLACEMENT;
}
