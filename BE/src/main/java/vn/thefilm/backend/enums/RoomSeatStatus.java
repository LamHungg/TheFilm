package vn.thefilm.backend.enums;

/**
 * Trạng thái của vị trí ghế trong sơ đồ phòng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum RoomSeatStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Đã loại khỏi sơ đồ ghế hiện hành. */
    REMOVED;
}
