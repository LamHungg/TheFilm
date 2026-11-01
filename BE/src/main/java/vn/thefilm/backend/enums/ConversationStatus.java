package vn.thefilm.backend.enums;

/**
 * Trạng thái của phiên hội thoại CSKH.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ConversationStatus {

    /** Đang mở để tiếp tục ghi nhận hoặc xử lý. */
    OPEN,

    /** Đang chờ phản hồi hoặc hành động tiếp theo. */
    WAITING,

    /** Đã đóng; không nhận nghiệp vụ mới. */
    CLOSED;
}
