# API nhân viên, quản lý vé và kho vé

Các API dưới đây dùng prefix `/api/v1/admin`, yêu cầu Bearer token và trả `ApiResponse<T>` hiện có. Request thay đổi dữ liệu có thể gửi `X-Request-Id` để nối audit. FE hiện chỉ có cổng khách hàng; tài liệu này là hợp đồng cho cổng quản trị.

## Phân quyền

| Chức năng | Vai trò |
|---|---|
| Nhân viên, phòng ban, chức vụ | `CHAIN_ADMIN`, `HR` |
| Tra cứu/kiểm tra vé | `CHAIN_ADMIN`, `CINEMA_MANAGER`, `CASHIER`, `USHER` |
| Soát vé | `CHAIN_ADMIN`, `CINEMA_MANAGER`, `USHER` |
| Xem kho ghế | `CHAIN_ADMIN`, `CINEMA_MANAGER`, `CASHIER` |
| Khóa/mở ghế | `CHAIN_ADMIN`, `CINEMA_MANAGER` |

Vé và kho ghế kiểm tra phạm vi rạp ở BE. Không truyền `cinemaId` khi tìm kiếm thì kết quả chỉ gồm các rạp được truy cập; `CHAIN_ADMIN` được xem toàn chuỗi. Chỉ định rạp ngoài phạm vi trả 403. Nhân sự giữ quyền quản lý tập trung của module hiện có.

Theo cơ chế quyền hiện có, phân công đang hiệu lực của nhân viên `ACTIVE` và phạm vi rạp gán riêng đều đóng góp vào danh sách rạp được truy cập. Gán phạm vi riêng qua `/employees/{id}/scopes` vẫn là thao tác độc lập; kết thúc phân công không xóa phạm vi riêng đã cấp. Chức vụ không tự cấp role Keycloak.

Các API phân trang dùng `page=0`, `size=20`, tối đa `size=100`. Response có `items`, `page`, `size`, `totalItems`, `totalPages`. Lỗi đầu vào trả 400, chưa xác thực 401, thiếu quyền 403, không tìm thấy 404, xung đột nghiệp vụ 409.

## Nhân viên và danh mục

| Method | Đường dẫn sau prefix | Nội dung |
|---|---|---|
| GET | `/employees?keyword=&status=` | Tìm kiếm cũ, giữ response danh sách |
| GET | `/employees/page?keyword=&status=&page=0&size=20` | Tìm kiếm phân trang, ID giảm dần |
| GET | `/employees/{id}` | `{employee, assignments}` |
| POST | `/employees` | Tạo hồ sơ `PENDING` |
| PUT | `/employees/{id}` | Sửa thông tin hồ sơ |
| PATCH | `/employees/{id}/status` | `{ "status": "ACTIVE" }` |
| GET | `/employees/{id}/assignments` | Lịch sử phân công |
| POST | `/employees/{id}/assignments` | Thêm phân công |
| POST | `/employees/{id}/assignments/{assignmentId}/end` | `{ "validTo": "2027-01-01" }` |
| POST | `/employees/{id}/link-user` | Liên kết OIDC theo API hiện có |
| POST | `/employees/{id}/scopes` | Gán phạm vi riêng theo API hiện có |
| GET | `/hr/departments?chainId=1` | Danh sách phòng ban |
| GET | `/hr/job-positions?chainId=1` | Danh sách chức vụ |
| POST | `/hr/departments` hoặc `/hr/job-positions` | Tạo danh mục |
| PUT | `/hr/departments/{id}` hoặc `/hr/job-positions/{id}` | Sửa hoặc ngừng hoạt động danh mục |

Body tạo/sửa nhân viên:

```json
{
  "chainId": 1,
  "employeeCode": "NV0100",
  "fullName": "Nguyễn Văn An",
  "dateOfBirth": "2000-02-15",
  "phone": "0901234567",
  "email": "an@example.com",
  "hiredOn": "2026-11-01"
}
```

`chainId`, `employeeCode`, `fullName` bắt buộc. Mã duy nhất trong chuỗi và không đổi sau khi tạo; không chuyển hồ sơ sang chuỗi khác. Ngày sinh phải trong quá khứ, ngày vào làm sau ngày sinh và không sau ngày nghỉ đã lưu. Trạng thái đổi sang `ACTIVE`, `ON_LEAVE`, `TERMINATED`; nghỉ việc lưu ngày nghỉ, kích hoạt lại xóa ngày nghỉ theo nghiệp vụ hiện có. Không có API xóa nhân viên.

Body phân công:

```json
{
  "cinemaId": 1,
  "departmentId": 1,
  "jobPositionId": 2,
  "validFrom": "2026-11-13",
  "validTo": null
}
```

Chỉ phân công nhân viên `ACTIVE`. `departmentId` tùy chọn; chức vụ bắt buộc. Rạp và danh mục phải cùng chuỗi, danh mục phải hoạt động. Khoảng ngày là `[validFrom, validTo)`, `null` nghĩa là chưa có ngày kết thúc. Cho phép đồng thời nhiều rạp, chặn chồng lấn trong cùng rạp và chặn bắt đầu trước ngày vào làm. Kết thúc chỉ được rút ngắn thời hạn, phải sau ngày bắt đầu, giữ bản ghi lịch sử. Phân công có ngày kết thúc tương lai vẫn hiệu lực đến ngày đó.

Body danh mục:

```json
{ "chainId": 1, "code": "USHER", "name": "Nhân viên soát vé", "status": "ACTIVE" }
```

Mã danh mục duy nhất trong chuỗi. Đổi `status` thành `INACTIVE` để ngừng dùng cho phân công mới; lịch sử giữ nguyên.

## Quản lý và soát vé

| Method | Đường dẫn sau prefix | Nội dung |
|---|---|---|
| GET | `/tickets` | Danh sách phân trang, ID giảm dần |
| GET | `/tickets/{id}` | Chi tiết vé và mã đơn liên quan |
| GET | `/tickets/{id}/scan-events?page=0&size=20` | Lịch sử quét tại rạp sở hữu vé, mới nhất trước |
| POST | `/tickets/validate` | Kiểm tra QR, không sử dụng vé hoặc ghi lịch sử quét |
| POST | `/tickets/scan` | Soát vé và ghi lịch sử |

Bộ lọc danh sách: `cinemaId`, `screeningId`, `date` (`yyyy-MM-dd`), `status`, `ticketNumber`, `orderNumber`, `page`, `size`. Mã vé và mã đơn đối chiếu chính xác sau khi bỏ khoảng trắng hai đầu. Ngày lọc theo giờ Việt Nam.

Body kiểm tra/soát vé:

```json
{
  "cinemaId": 1,
  "qrData": "THEFILM:TICKET:<ticketNumber>:<qrTokenHash>",
  "deviceReference": "gate-01"
}
```

`cinemaId`, `qrData` bắt buộc; `deviceReference` tùy chọn. Giữ tương thích QR do luồng đặt vé hiện tại phát hành. API quản trị không trả `qrData`/`qrTokenHash`. Lịch sử chỉ lưu SHA-256 của toàn bộ chuỗi QR, không lưu lại credential có thể dùng để vào rạp.

Response nghiệp vụ trong `data`:

```json
{
  "result": "ACCEPTED",
  "reason": "VALID",
  "ticket": {
    "ticketId": 1000,
    "ticketNumber": "T...",
    "status": "USED"
  }
}
```

`ticket` còn có rạp, suất, phim, phòng, ghế, mã đơn, giờ chiếu, thời điểm phát hành/quét và người quét. Ví dụ trên rút gọn các trường để dễ đọc.

| result | reason | Ý nghĩa |
|---|---|---|
| `ACCEPTED` | `VALID` | Vé hợp lệ, scan chuyển `VALID → USED` |
| `ALREADY_USED` | `ALREADY_USED` | Đã dùng, không nhận vào lần hai |
| `CANCELLED` | `CANCELLED` hoặc `REFUNDED` | Vé/đơn/suất bị hủy hoặc đã hoàn |
| `EXPIRED` | `EXPIRED` | Vé hết hạn hoặc qua ngày chiếu |
| `INVALID` | `INVALID_QR`, `NOT_YET_VALID`, `ORDER_NOT_ISSUED` | QR sai, chưa tới ngày, hoặc đơn chưa đủ điều kiện |
| `WRONG_CINEMA` | `WRONG_CINEMA` | Vé thuộc rạp khác |

Những kết quả từ chối soát vé vẫn trả HTTP 200 với `data.result`; FE phải đọc trường này để quyết định mở cổng. QR sai và sai rạp không trả thông tin vé. Mọi lần gọi scan đã qua xác thực/phân quyền và validation đều lưu kết quả, kể cả bị từ chối. Lượt quét sai rạp không xuất hiện trong lịch sử của rạp sở hữu vé để tránh lộ thông tin nhân viên/thiết bị rạp khác.

Vé chỉ được nhận trong ngày chiếu theo `Asia/Ho_Chi_Minh`, không giới hạn theo giờ bắt đầu/kết thúc. Cần đơn đã thanh toán và phát hành, vé `VALID`, suất chưa hủy. Hai máy cùng quét chỉ có một lượt `ACCEPTED`; vé đã sử dụng không được đặt lại trạng thái bằng API này.

## Kho ghế theo suất

| Method | Đường dẫn sau prefix | Nội dung |
|---|---|---|
| GET | `/ticket-inventory` | Thống kê phân trang, giờ chiếu giảm dần |
| GET | `/ticket-inventory/screenings/{id}` | `{screening, seats}` |
| POST | `/ticket-inventory/screenings/{id}/block` | Khóa các ghế trống |
| POST | `/ticket-inventory/screenings/{id}/unblock` | Mở các ghế bị khóa thủ công |

Bộ lọc danh sách: `cinemaId`, `filmId`, `date`, `status`, `page`, `size`. Thống kê trả `total`, `available`, `held`, `sold`, `blocked`; `total` bao gồm ghế bị khóa và bằng tổng bốn trạng thái. Chi tiết ghế có ID, nhãn/hàng/loại ghế, trạng thái, ID đơn đang giữ/bán, hạn giữ và `manuallyBlocked`. Không trả hold token hoặc QR.

Body khóa/mở:

```json
{ "showSeatIds": [1, 2], "reason": "Tạm khóa để kiểm tra ghế" }
```

Danh sách 1–500 ID dương, không trùng, cùng suất; lý do bắt buộc, tối đa 1000 ký tự. Chỉ khóa ghế `AVAILABLE`; chỉ mở ghế `BLOCKED` có dấu khóa thủ công và còn bán được, không có bảo trì giao với suất. Không sửa suất đã kết thúc/hủy. Ghế đang giữ, kể cả giữ hết hạn chưa được scheduler giải phóng, vẫn bị từ chối. Cả lô thành công hoặc hoàn tác toàn bộ. Ghế khóa được luồng giữ ghế hiện có từ chối ngay.

## Database và kiểm thử

Không thêm bảng, cột, constraint hoặc migration. Các module dùng schema Oracle V001-V005 có sẵn: `department`, `job_position`, `employee`, `employee_assignment`, `ticket`, `ticket_scan_event`, `show_seat`, `audit_event` và các bảng catalog liên quan. Entity mới chỉ ánh xạ các bảng đã tồn tại.

Khóa thủ công chỉ cập nhật `show_seat.seat_status` và ghi lý do vào `audit_event.after_data` hiện có. BE xác định `manuallyBlocked` qua sự kiện mới nhất của ghế: chỉ `SHOW_SEAT_BLOCKED` mới cho phép mở lại. Ghế BLOCKED cũ, không có audit hoặc không xác minh được nguồn khóa sẽ không được mở bằng API này. Audit phải được giữ để phục vụ thao tác mở ghế; không phát sinh trường lưu trữ mới.

Schema và seed H2 trong `src/main/resources/db/local` được giữ nguyên. Vì bản local hiện có chưa đủ các bảng/cột Oracle mà entity đã ánh xạ, kiểm thử dùng fixture riêng trong `src/test/resources/db/admin-fixture`, không thay đổi schema ứng dụng hoặc database thật. Fixture có phòng ban ID 1, chức vụ quản lý ID 1 và soát vé ID 2. Chạy kiểm thử và chạy ứng dụng với schema Oracle hiện có:

```powershell
cd BE
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

Profile local không giả lập role quản trị; các endpoint quản trị vẫn yêu cầu xác thực và đúng role. Với môi trường Oracle phát triển, dùng Keycloak hoặc profile `oracle,devauth` hiện có. Profile `devauth` dùng `Bearer mock-admin-token`, không có actor ID thực trong audit; chỉ dùng cho phát triển. Khi chạy bằng Keycloak, liên kết nhân viên với `app_user` để ghi nhận actor thực.

`AdminOperationsIntegrationTests` kiểm tra hồ sơ/danh mục/phân công, trạng thái và ngày soát vé, QR sai, quyền chức năng/phạm vi rạp, không lộ credential, khóa ghế theo lô, bảo trì, ghế đã bán, quét vé đồng thời và khóa ghế cạnh tranh với giữ ghế. Bộ kiểm thử hiện có tiếp tục chạy để kiểm tra hồi quy đặt vé/xác thực.

Chưa bao gồm ca làm, chấm công, nghỉ phép, bảng lương, bán tại quầy và hủy/hoàn vé.
