package vn.thefilm.backend.enums;

/**
 * Phạm vi áp dụng của một cấu hình hệ thống.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum AppSettingScope {

    /** Áp dụng toàn hệ thống. */
    GLOBAL,

    /** Áp dụng cho một chuỗi rạp. */
    CHAIN,

    /** Áp dụng cho một pháp nhân kinh doanh. */
    ENTITY,

    /** Áp dụng riêng cho một rạp. */
    CINEMA;
}
