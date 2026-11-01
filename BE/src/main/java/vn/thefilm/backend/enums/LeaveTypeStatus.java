package vn.thefilm.backend.enums;

/**
 * Trạng thái sử dụng của danh mục loại nghỉ.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum LeaveTypeStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Đã ngừng hoạt động; vẫn giữ dữ liệu để tra cứu lịch sử. */
    INACTIVE;
}
