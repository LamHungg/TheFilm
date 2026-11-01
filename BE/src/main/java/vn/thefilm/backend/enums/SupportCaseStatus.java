package vn.thefilm.backend.enums;

/**
 * Trạng thái xử lý vụ việc chăm sóc khách hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum SupportCaseStatus {

    /** Vụ việc vừa được tạo, chưa có người xử lý. */
    NEW,

    /** Chatbot AI đang xử lý hội thoại. */
    AI_HANDLING,

    /** Đang chờ nhân viên CSKH tiếp nhận. */
    WAITING_AGENT,

    /** Đang được nhân viên tiếp nhận và xử lý. */
    IN_PROGRESS,

    /** Đã có hướng xử lý và được đánh dấu giải quyết. */
    RESOLVED,

    /** Đã đóng; không nhận nghiệp vụ mới. */
    CLOSED;
}
