package vn.thefilm.backend.enums;

/**
 * Mức độ ưu tiên của vụ việc CSKH.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum SupportPriority {

    /** Mức ưu tiên thấp. */
    LOW,

    /** Mức ưu tiên thông thường. */
    NORMAL,

    /** Mức ưu tiên cao. */
    HIGH,

    /** Cần được xử lý khẩn cấp. */
    URGENT;
}
