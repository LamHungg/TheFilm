package vn.thefilm.backend.enums;

/**
 * Kết quả xử lý của một lượt tương tác với chatbot AI.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum AiInteractionStatus {

    /** AI đã tạo câu trả lời cho khách. */
    ANSWERED,

    /** AI chuyển hội thoại cho nhân viên tiếp tục xử lý. */
    HANDOFF,

    /** Bị khóa hoặc bị chặn, không được phép sử dụng. */
    BLOCKED,

    /** Đã phát sinh lỗi khi xử lý. */
    ERROR;
}
