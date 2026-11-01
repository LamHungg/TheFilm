package vn.thefilm.backend.enums;

/**
 * Trạng thái đồng ý hoặc rút lại sự đồng ý của khách hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum CustomerConsentStatus {

    /** Khách hàng đã đồng ý. */
    GRANTED,

    /** Khách hàng đã rút lại sự đồng ý trước đó. */
    WITHDRAWN,

    /** Khách hàng không đồng ý. */
    DECLINED;
}
