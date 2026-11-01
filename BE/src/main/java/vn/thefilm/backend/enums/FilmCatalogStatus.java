package vn.thefilm.backend.enums;

/**
 * Trạng thái phim suy ra để hiển thị trên cổng khách hàng.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum FilmCatalogStatus {

    /** Có suất chiếu đang mở bán cho khách hàng. */
    NOW_SHOWING,

    /** Có lịch chiếu sắp tới nhưng chưa mở bán. */
    COMING_SOON,

    /** Phim đã được duyệt nhưng chưa có lịch chiếu phù hợp để xếp vào đang chiếu hoặc sắp chiếu. */
    APPROVED;
}
