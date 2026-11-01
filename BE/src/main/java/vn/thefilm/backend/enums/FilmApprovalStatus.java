package vn.thefilm.backend.enums;

/**
 * Trạng thái biên tập và phê duyệt nội dung phim.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum FilmApprovalStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Phim đã được duyệt và đủ điều kiện công bố. */
    APPROVED,

    /** Phim đã lưu trữ, không còn hiển thị trong danh mục bán vé. */
    ARCHIVED;
}
