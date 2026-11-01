package vn.thefilm.backend.enums;

/**
 * Kiểu dữ liệu của giá trị cấu hình hệ thống.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum AppSettingType {

    /** Giá trị cấu hình dạng văn bản. */
    STRING,

    /** Giá trị cấu hình dạng số. */
    NUMBER,

    /** Giá trị cấu hình đúng/sai. */
    BOOLEAN,

    /** Giá trị cấu hình dạng cấu trúc JSON. */
    JSON;
}
