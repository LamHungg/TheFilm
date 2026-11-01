package vn.thefilm.backend.enums;

/**
 * Vai trò gửi một tin nhắn trong hội thoại hỗ trợ.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum MessageSenderType {

    /** Tin nhắn do khách hàng gửi. */
    CUSTOMER,

    /** Tin nhắn do nhân viên CSKH gửi. */
    AGENT,

    /** Tin nhắn hoặc kết quả do chatbot AI tạo. */
    AI,

    /** Tin nhắn tự động do hệ thống phát sinh. */
    SYSTEM;
}
