package vn.thefilm.backend.enums;

/**
 * Trạng thái duyệt dùng cho quy tắc giá và cấu hình thuế.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ApprovalStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Đã được người có thẩm quyền phê duyệt. */
    APPROVED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Đã ngừng áp dụng nhưng được giữ để bảo toàn lịch sử. */
    RETIRED;
}
