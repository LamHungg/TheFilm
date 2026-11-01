package vn.thefilm.backend.enums;

/**
 * Loại chính sách mà khách hàng đưa ra lựa chọn đồng ý.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum CustomerConsentType {

    /** Chính sách điều khoản sử dụng. */
    TERMS,

    /** Chính sách quyền riêng tư và xử lý dữ liệu cá nhân. */
    PRIVACY,

    /** Sự đồng ý nhận thông tin tiếp thị. */
    MARKETING,

    /** Sự đồng ý lưu hoặc sử dụng cookie. */
    COOKIE,

    /** Sự đồng ý xử lý nội dung hội thoại bằng AI. */
    AI_PROCESSING;
}
