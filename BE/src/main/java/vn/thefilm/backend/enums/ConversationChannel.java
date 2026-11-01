package vn.thefilm.backend.enums;

/**
 * Kênh tiếp nhận hội thoại chăm sóc khách hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ConversationChannel {

    /** Hội thoại bắt đầu từ website. */
    WEB,

    /** Hội thoại bắt đầu từ kênh di động. */
    MOBILE,

    /** Hội thoại đến từ mạng xã hội. */
    SOCIAL,

    /** Yêu cầu hỗ trợ qua điện thoại. */
    PHONE,

    /** Yêu cầu hỗ trợ qua email. */
    EMAIL;
}
