package vn.thefilm.backend.enums;

/**
 * Trạng thái bật/tắt dùng chung cho các danh mục có thể ngừng hoạt động.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ActiveStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Đã ngừng hoạt động; vẫn giữ dữ liệu để tra cứu lịch sử. */
    INACTIVE;
}
