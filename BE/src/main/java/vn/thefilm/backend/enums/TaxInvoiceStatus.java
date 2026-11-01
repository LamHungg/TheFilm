package vn.thefilm.backend.enums;

/**
 * Trạng thái vòng đời hóa đơn giả lập.
 * Tên mỗi hằng số khớp mã cố định được lưu trong schema THEFILM.
 */
public enum TaxInvoiceStatus {

    /** Bản nháp, chưa gửi duyệt hoặc công bố. */
    DRAFT,

    /** Đang chờ xử lý hoặc phê duyệt. */
    PENDING,

    /** Hóa đơn giả lập đã được nhà cung cấp xác nhận tiếp nhận. */
    ACCEPTED,

    /** Đã bị từ chối và không được tiếp tục theo luồng hiện tại. */
    REJECTED,

    /** Quá thời gian chờ phản hồi; cần đối soát trước khi kết luận. */
    TIMEOUT,

    /** Hóa đơn đã được điều chỉnh; bản ghi và dấu vết cũ vẫn được giữ. */
    ADJUSTED,

    /** Quy trình hoặc giao dịch đã bị hủy. */
    CANCELLED;
}
