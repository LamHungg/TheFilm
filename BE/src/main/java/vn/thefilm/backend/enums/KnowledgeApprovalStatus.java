package vn.thefilm.backend.enums;

/**
 * Trạng thái duyệt bài tri thức dùng cho CSKH và AI.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum KnowledgeApprovalStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã ngừng áp dụng nhưng được giữ để bảo toàn lịch sử. */
    RETIRED;
}
