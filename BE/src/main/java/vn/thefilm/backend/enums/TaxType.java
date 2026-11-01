package vn.thefilm.backend.enums;

/**
 * Nhóm sắc thuế được mô phỏng trong hệ thống.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TaxType {

    /** Thuế giá trị gia tăng. */
    VAT,

    /** Thuế thu nhập cá nhân. */
    PIT,

    /** Thuế thu nhập doanh nghiệp. */
    CIT,

    /** Nhóm khác, dùng khi không thuộc các loại đã định nghĩa. */
    OTHER;
}
