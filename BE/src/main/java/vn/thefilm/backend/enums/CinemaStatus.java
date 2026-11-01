package vn.thefilm.backend.enums;

/**
 * Trạng thái vận hành của địa điểm rạp.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum CinemaStatus {

    /** Đang hoạt động và được phép phát sinh nghiệp vụ mới. */
    ACTIVE,

    /** Đã ngừng hoạt động; vẫn giữ dữ liệu để tra cứu lịch sử. */
    INACTIVE,

    /** Rạp đã đóng cửa và ngừng vận hành. */
    CLOSED;
}
