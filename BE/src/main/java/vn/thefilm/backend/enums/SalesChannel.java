package vn.thefilm.backend.enums;

/**
 * Kênh ghi nhận đơn hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum SalesChannel {

    /** Đơn được tạo trên website hoặc kênh trực tuyến. */
    ONLINE,

    /** Đơn được nhân viên bán tại quầy. */
    BOX,

    /** Đơn được tạo tại máy bán vé tự phục vụ. */
    KIOSK,

    /** Đơn được tạo từ cổng quản trị. */
    ADMIN;
}
