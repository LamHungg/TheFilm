package vn.thefilm.backend.enums;

/**
 * Nhóm khoản cấu thành dòng lương.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PayrollLineType {

    /** Khoản thu nhập làm tăng lương thực nhận. */
    EARNING,

    /** Khoản khấu trừ khỏi lương. */
    DEDUCTION,

    /** Khoản thuế được khấu trừ hoặc tính trong bảng lương. */
    TAX;
}
