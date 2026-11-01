package vn.thefilm.backend.enums;

/**
 * Trạng thái tiếp nhận và xử lý callback thanh toán.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum PaymentCallbackStatus {

    /** Callback đã được tiếp nhận. */
    RECEIVED,

    /** Callback đã được xử lý thành công. */
    PROCESSED,

    /** Callback được lưu nhưng bỏ qua vì không cần xử lý. */
    IGNORED,

    /** Đã phát sinh lỗi khi xử lý. */
    ERROR;
}
