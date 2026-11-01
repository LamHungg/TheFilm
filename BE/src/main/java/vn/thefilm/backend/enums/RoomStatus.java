package vn.thefilm.backend.enums;

/**
 * Trạng thái hoạt động hoặc bảo trì của phòng chiếu.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum RoomStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Tạm ngừng sử dụng để bảo trì. */
    MAINTENANCE,

    /** Đã ngừng hoạt động; vẫn giữ dữ liệu để tra cứu lịch sử. */
    INACTIVE;
}
