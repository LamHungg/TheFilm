package vn.thefilm.backend.enums;

/**
 * Trạng thái phân ca làm việc của nhân viên.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum ShiftAssignmentStatus {

    /** Ca đã được lên kế hoạch nhưng chưa xác nhận. */
    PLANNED,

    /** Việc giữ ghế hoặc ca làm đã được xác nhận. */
    CONFIRMED,

    /** Quy trình đã hoàn tất. */
    COMPLETED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
