package vn.thefilm.backend.enums;

/**
 * Trạng thái phiên bản sơ đồ ghế của phòng chiếu.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum RoomLayoutStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Đã ngừng áp dụng nhưng được giữ để bảo toàn lịch sử. */
    RETIRED;
}
