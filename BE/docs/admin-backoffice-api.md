# API cổng quản trị: phần còn thiếu cho FE admin

Tài liệu này là hợp đồng API cho các trang admin FE đã dựng xong trên mock (nhánh `ui/admin`). Nó bổ sung cho [admin-operations.md](admin-operations.md) (nhân viên, soát vé, kho ghế), không lặp lại phần đó.

Đọc theo thứ tự:

1. **Quy ước chung**: giữ nguyên quy ước API admin hiện có.
2. **Cần chốt trước khi code**: những chỗ màn hình FE cần dữ liệu mà schema V001–V005 chưa có. Phải chốt các mục này trước, vì một số mục cần migration.
3. **Bảng tổng**: mỗi trang FE dùng API nào, cái nào đã có, cái nào còn thiếu.
4. **Đặc tả từng module**.

Schema là chuẩn. Ở chỗ FE mock lệch schema (tên trạng thái, cấu trúc), API trả theo schema, FE sẽ đổi lại cho khớp. Các chỗ lệch được ghi trong mục **FE đang dùng** của từng module.

---

## 1. Quy ước chung

Giữ nguyên quy ước của `admin-operations.md`:

- Prefix `/api/v1/admin`, Bearer token, trả `ApiResponse<T>` (`success`, `data`, `error{code,message}`, `timestamp`).
- Phân trang: `page=0`, `size=20`, tối đa `size=100`. `data` là `PageResponse` (`items`, `page`, `size`, `totalItems`, `totalPages`). Danh sách nhỏ, cố định (loại phòng, hạng thành viên, mẫu ca) trả `List<T>` không phân trang.
- Mã lỗi: 400 đầu vào sai, 401 chưa xác thực, 403 thiếu quyền hoặc ngoài phạm vi rạp, 404 không tìm thấy, 409 xung đột nghiệp vụ (trùng mã, sai trạng thái chuyển). `error.code` là mã máy đọc được, ví dụ `PRICE_RULE_CODE_DUPLICATE`; `error.message` là câu tiếng Việt để FE hiện thẳng cho người dùng.
- Thao tác ghi nhận header `X-Request-Id`, ghi `audit_event` bằng `AdminAudit` như các service admin hiện có.
- **Phạm vi rạp**: endpoint nào có `cinemaId` thì kiểm tra bằng `AdminScopeGuard`. Không truyền `cinemaId` thì chỉ trả các rạp người gọi được truy cập; `CHAIN_ADMIN` thấy toàn chuỗi.
- **Tiền**: số VND kiểu `number`, không chuỗi định dạng. **Thời điểm**: ISO-8601 có offset (`2026-11-14T19:30:00+07:00`). **Ngày**: `yyyy-MM-dd`, hiểu theo `Asia/Ho_Chi_Minh`. **Giờ trong ngày**: `HH:mm`.
- Trường `Y`/`N` trong DB trả về FE dạng `boolean`.
- Không xoá cứng dữ liệu nghiệp vụ. "Xoá" trên FE nghĩa là đổi `status` sang `INACTIVE`, `CANCELLED` hoặc `RETIRED`. Riêng nháp chưa từng dùng (quy tắc giá `DRAFT`, voucher chưa phát) được xoá thật nếu BE thấy cần.

**Vai trò.** Keycloak hiện có `CHAIN_ADMIN`, `CINEMA_MANAGER`, `HR`, `CASHIER`, `USHER`. Các module mới dùng các vai trò này. Vai trò `SUPPORT` và `ACCOUNTANT` được đề xuất ở mục 2.

---

## 2. Cần chốt trước khi code

Màn hình FE cần các dữ liệu dưới đây, nhưng schema chưa có cột tương ứng. Mỗi dòng có một **đề xuất**. Sau khi chốt thì sửa đúng dòng đó trong tài liệu này, rồi FE đổi theo.

| # | Màn hình cần | Schema hiện có | Đề xuất |
| --- | --- | --- | --- |
| C1 | Giá vé theo **khung giờ** (trước 17h, sau 17h) | `price_rule` có `weekday_no`, `screening_type_code`, không có giờ | **Migration** thêm `start_time VARCHAR2(5)`, `end_time VARCHAR2(5)` (null = cả ngày). Không có thì FE bỏ cột giờ |
| C2 | Giá **ngày lễ** | Không có lịch ngày lễ | Ngày lễ là quy tắc có `valid_from`/`valid_to` đúng ngày đó, `priority_no` cao. Không cần bảng mới |
| C3 | **Phụ thu** theo loại phòng, loại ghế | `room_type`, `seat_type` không có cột tiền | Bỏ phụ thu khỏi FE. Giá đặc thù viết thành `price_rule` có `room_type_id`/`seat_type_id`. Vẫn cần hiển thị thì lưu trong `room_type.attributes_json` `{ "surcharge": 60000 }`, chỉ để hiển thị |
| C4 | Ghế đôi "2 chỗ / ghế" | `room_seat.companion_group` | Dùng `companion_group`: hai ghế cùng nhóm là một ghế đôi. Bỏ `seatsPerUnit` khỏi FE |
| C5 | **Tỉ lệ chia doanh thu** cho nhà phát hành; trạng thái quyền "Chờ duyệt", "Hết hạn" | `film_release_right` không có cột tỉ lệ; trạng thái chỉ `ACTIVE`/`INACTIVE` | **Migration** thêm `revenue_share_pct NUMBER(5,2)`. "Hết hạn" suy ra từ `valid_to < now`, không lưu. "Chờ duyệt": tạm dùng `INACTIVE` |
| C6 | Hotline, website của chuỗi; người đại diện, địa chỉ của pháp nhân | `org_chain` chỉ có mã, tên. Địa chỉ nằm ở `legal_entity_profile`, không có người đại diện | Địa chỉ, email hoá đơn lấy từ `legal_entity_profile` bản đang hiệu lực. Hotline, website lưu `app_setting` scope `CHAIN`. Người đại diện: **migration** thêm `legal_representative` vào `legal_entity_profile`, hoặc bỏ khỏi FE |
| C7 | Hạng thành viên theo **chi tiêu** và **tỉ lệ tích điểm** | `member_tier.minimum_points`, `benefits_json` | Xét hạng theo điểm (`minimum_points`). FE đổi "Chi tiêu tối thiểu" thành "Điểm tối thiểu". Tỉ lệ tích điểm và mô tả quyền lợi lưu trong `benefits_json` `{ "earnRatePct": 7, "perks": "…" }` |
| C8 | Chủ đề (topic) của bài hướng dẫn CSKH | `knowledge_article` không có cột chủ đề | Quy ước tiền tố `article_code`: `BOOKING-…`, `REFUND-…`, `MEMBER-…`, `PAYMENT-…`, `FNB-…`. API trả thêm `topic` suy ra từ tiền tố |
| C9 | Vai trò CSKH, kế toán | Keycloak chưa có | Thêm role `SUPPORT` (CSKH) và `ACCOUNTANT` (hoá đơn, báo cáo, lương). Chưa thêm thì cho `CHAIN_ADMIN` làm hết |
| C10 | Gửi hoá đơn điện tử thật | `tax_provider_attempt.provider_code = 'MOCK'` (CHECK) | Giai đoạn này chỉ làm `MOCK`. Nối nhà cung cấp thật thì cần migration nới CHECK |
| C11 | Cài đặt hệ thống (giữ ghế, hoàn vé, cổng thanh toán…) | `app_setting` dạng khoá–giá trị | Dùng các khoá ở mục 4.14. Không cần bảng mới |

---

## 3. Bảng tổng

Trạng thái: **Có** là API đã có đủ, FE chỉ cần nối. **Thiếu** là đã có một phần, cần bổ sung. **Mới** là chưa có gì.

| Trang FE (route) | Thuộc tính trong `AdminBackofficeService` / service cũ | API | Trạng thái | Bảng DB |
| --- | --- | --- | --- | --- |
| Tổng quan `/dashboard` | `AdminOpsService.dashboardStats`, `cinemaOccupancy` | `GET /dashboard/*` (4.13) | Mới | tổng hợp |
| Phim `/films` | `AdminCatalogService.films` | `/films` | Có | `film` |
| Bản chiếu & phát hành `/film-releases` | `filmVersions`, `releaseRights` | `/films/{id}/versions`, `/release-rights` (4.3) | Thiếu | `film_version`, `film_release_right` |
| Rạp & phòng `/cinemas` | `AdminCatalogService.cinemas`, `rooms` | `/cinemas`, `/cinemas/{id}/rooms` (4.2) | Thiếu: sửa phòng | `cinema`, `cinema_room` |
| Sơ đồ ghế `/cinemas/rooms/:roomId` | `layoutOf`, `saveLayout`, `seatMaintenance` | `/rooms/{id}/layouts`, `/layouts/{id}/seats`, `/seats/{id}/maintenance` (4.2) | Thiếu: lưu theo lô | `room_layout`, `room_seat`, `seat_maintenance` |
| Loại phòng & ghế `/room-config` | `roomTypes`, `seatTypes`, `seatMaintenance` | `/room-types`, `/seat-types`, `/maintenance` (4.2) | Thiếu: sửa, danh sách bảo trì | `room_type`, `seat_type`, `seat_maintenance` |
| Suất chiếu `/screenings` | `AdminCatalogService.screenings` | `/screenings` | Có | `screening` |
| Giá vé `/pricing` | `priceRules` | `/price-rules` (4.4) | Thiếu: sửa | `price_rule` |
| Báo cáo `/reports` | (mock trong component) | `GET /reports/*` (4.13) | Mới | tổng hợp |
| Đơn hàng `/orders` | `AdminOpsService.orders` | `/orders` (4.5) | Mới | `sales_order`, `sales_order_line`, `payment_txn` |
| Hoàn & đổi vé `/refunds` | `refundRequests` | `/refund-requests`, `/ticket-changes` (4.6) | Mới | `refund_request*`, `refund_txn`, `ticket_change_request` |
| Khuyến mãi `/promotions` | `AdminOpsService.promotions`, `vouchers` | `/promo-campaigns`, `/vouchers` (4.7) | Mới | `promo_campaign`, `promo_voucher`, `promo_redemption` |
| Thành viên `/members` | `members`, `memberTiers`, `pointsLedger` | `/members`, `/member-tiers` (4.8) | Mới | `member_*`, `app_user`, `customer_profile` |
| Bắp nước `/fnb` | `fnbProducts`, `fnbCategories`, `inventory`, `inventoryTxns` | `/fnb/*`, `/inventory/*` (4.9) | Mới | `fnb_*`, `inventory_*` |
| Hoá đơn `/invoices` | `taxInvoices`, `taxRules` | `/tax-invoices`, `/tax-rules` (4.10) | Mới | `tax_*` |
| CSKH `/support` | `AdminOpsService.supportCases`, `supportMessages`, `knowledge` | `/support-cases`, `/knowledge-articles` (4.12) | Mới | `support_*`, `knowledge_article` |
| Nhân viên & phân quyền `/staff` | `employees` | `/employees/*` (admin-operations.md) + 4.11.1 | Thiếu: vai trò Keycloak | `employee`, `user_cinema_scope` |
| Ca làm & lương `/shifts` | `shiftTemplates`, `shiftAssignments`, `attendance`, `leaveRequests`, `payroll` | `/shift-*`, `/attendance`, `/leave-*`, `/payroll/*` (4.11) | Mới | `shift_*`, `attendance_*`, `leave_*`, `payroll_*` |
| Tổ chức `/organization` | `chains`, `legalEntities` | `/chains`, `/legal-entities` (4.1) | Thiếu: sửa, hồ sơ | `org_chain`, `legal_entity*` |
| Nhật ký thao tác `/audit` | `audit` | `GET /audit-events` (4.14) | Mới | `audit_event` |
| Cài đặt `/settings` | `settings` | `/settings` (4.14) | Mới | `app_setting` |

---

## 4. Đặc tả từng module

Mỗi module có: endpoint, quyền, response mẫu, luật nghiệp vụ. Bảng endpoint ghi đường dẫn **sau prefix**.

### 4.1 Tổ chức

| Method | Đường dẫn | Nội dung | Trạng thái |
| --- | --- | --- | --- |
| GET | `/chains` | Danh sách chuỗi | Có |
| POST | `/chains` | Tạo chuỗi | Có |
| PUT | `/chains/{id}` | Sửa tên, trạng thái; hotline, website ghi vào `app_setting` scope `CHAIN` (C6) | Mới |
| GET | `/legal-entities?chainId=` | Danh sách pháp nhân **kèm hồ sơ đang hiệu lực** | Thiếu: thêm `profile` |
| POST | `/legal-entities` | Tạo pháp nhân và hồ sơ đầu tiên | Thiếu: nhận `profile` |
| PUT | `/legal-entities/{id}` | Sửa tên, mã số thuế, trạng thái. Đổi `profile` thì **tạo phiên bản hồ sơ mới**: đóng `valid_to` bản cũ, `version_no + 1` | Mới |

Quyền: `CHAIN_ADMIN`.

```json
{
  "legalEntityId": 1,
  "chainId": 1,
  "entityCode": "TF-HCM",
  "legalName": "Công ty TNHH Giải trí THEFILM Sài Gòn",
  "taxIdentifier": "0316789012",
  "status": "ACTIVE",
  "profile": {
    "versionNo": 3,
    "validFrom": "2026-01-01T00:00:00+07:00",
    "displayName": "THEFILM Sài Gòn",
    "registeredAddress": "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh",
    "invoiceEmail": "hoadon@thefilm.vn",
    "legalRepresentative": "Nguyễn Minh Quân"
  }
}
```

`legalRepresentative` chỉ có khi C6 chọn migration. Mã số thuế duy nhất trong chuỗi. Không ngừng hoạt động được pháp nhân khi còn rạp `ACTIVE` gắn với nó: trả 409 `LEGAL_ENTITY_IN_USE`.

**FE đang dùng:** `representative`, `address` ở cấp pháp nhân (đổi sang đọc `profile.*`); `hotline`, `website` ở cấp chuỗi.

### 4.2 Rạp, phòng, loại phòng, loại ghế, sơ đồ ghế, bảo trì

Phần lớn đã có ở `AdminOrgController` và `AdminRoomController`. Chỉ cần bổ sung:

| Method | Đường dẫn | Nội dung | Trạng thái |
| --- | --- | --- | --- |
| PUT | `/rooms/{roomId}` | Sửa tên, loại phòng của phòng chiếu | Mới |
| PUT | `/room-types/{id}`, `/seat-types/{id}` | Sửa tên, mô tả (mã không đổi) | Mới |
| PUT | `/layouts/{layoutId}/seats` | **Lưu cả sơ đồ một lần**: danh sách `{ roomSeatId, seatTypeId, sellable, companionGroup }` | Mới |
| GET | `/maintenance?cinemaId=&status=&page=` | Bảo trì ghế mọi phòng, cho tab "Bảo trì ghế" | Mới |

Quyền: `CHAIN_ADMIN`, `CINEMA_MANAGER` trong phạm vi rạp. Loại phòng, loại ghế: `CHAIN_ADMIN`.

Lưu sơ đồ theo lô:

- Cả lô thành công hoặc không gì cả. Chỉ sửa layout `DRAFT`. Layout `ACTIVE` đã có suất bán thì trả 409 `LAYOUT_IN_USE`. FE sẽ tạo layout mới (`POST /rooms/{id}/layouts`), sửa, rồi `activate`.
- Ô "Lối đi" trên FE là ghế `sellable = false`. Không tạo, không xoá `room_seat` qua API này.
- Ghế đôi: hai ghế liền nhau cùng `companionGroup` (C4). Không đúng một cặp liền nhau thì trả 400.
- Báo hỏng hoặc sửa xong **không đi qua API này**. FE gọi `POST /seats/{id}/maintenance` và `POST /maintenance/{id}/resolve` cho từng ghế đổi trạng thái hỏng.

Dòng bảo trì trả thêm tên rạp, tên phòng, nhãn ghế để FE không phải gọi thêm:

```json
{
  "seatMaintenanceId": 1,
  "roomSeatId": 77,
  "seatLabel": "E7",
  "roomId": 1,
  "roomName": "IMAX 1",
  "cinemaId": 1,
  "cinemaName": "THEFILM Vincom Đồng Khởi",
  "reason": "Tay vịn gãy",
  "startAt": "2026-11-12T19:30:00+07:00",
  "endAt": null,
  "status": "OPEN"
}
```

**FE đang dùng:** phụ thu ở loại phòng, loại ghế (C3); `seatsPerUnit` (C4); bảo trì có `seatLabel = "Toàn phòng"`. Đóng cả phòng thì dùng `PATCH /rooms/{id}/status` `MAINTENANCE`, không tạo phiếu bảo trì ghế.

### 4.3 Bản chiếu và quyền phát hành

| Method | Đường dẫn | Nội dung | Trạng thái |
| --- | --- | --- | --- |
| GET | `/film-versions?filmId=&formatCode=&status=&page=` | Bản chiếu mọi phim, kèm `filmTitle` | Mới |
| POST | `/films/{id}/versions` | Tạo | Có |
| PUT | `/film-versions/{id}` | Sửa định dạng, ngôn ngữ, phụ đề, thời lượng, trạng thái | Mới |
| GET | `/release-rights?filmId=&cinemaId=&state=&page=` | Quyền phát hành mọi phim. `state` = `ACTIVE`, `UPCOMING`, `EXPIRED`, `INACTIVE` (suy từ ngày, C5) | Mới |
| POST | `/films/{id}/release-rights` | Tạo | Có |
| PUT | `/release-rights/{id}` | Sửa thời hạn, phạm vi, nhà phát hành, tỉ lệ chia | Mới |

Quyền: `CHAIN_ADMIN`.

```json
{
  "filmVersionId": 1,
  "filmId": 1,
  "filmTitle": "Tầng Mây Cuối Cùng",
  "versionCode": "TMCC-IMAX-SUB",
  "formatCode": "IMAX",
  "languageCode": "en",
  "subtitleCode": "vi",
  "runtimeMinutes": 136,
  "status": "ACTIVE"
}
```

```json
{
  "releaseRightId": 4,
  "filmId": 4,
  "filmTitle": "Hồ Sơ Đêm Mưa",
  "cinemaId": 1,
  "cinemaName": "THEFILM Vincom Đồng Khởi",
  "distributorName": "Beta Media",
  "referenceCode": "HD-2026-118",
  "validFrom": "2026-11-15T00:00:00+07:00",
  "validTo": "2026-12-31T23:59:59+07:00",
  "revenueSharePct": 55,
  "status": "ACTIVE",
  "state": "UPCOMING"
}
```

`cinemaId = null` nghĩa là toàn chuỗi. Hai quyền `ACTIVE` của cùng phim, cùng phạm vi không được chồng thời gian: 409 `RELEASE_RIGHT_OVERLAP`. Tạo suất chiếu ngoài thời hạn quyền phát hành đã bị `/screenings` chặn (`AdminScreeningServiceImpl`). Sửa quyền phát hành không được làm các suất đã mở bán rơi ra ngoài thời hạn: trả 409 `RELEASE_RIGHT_HAS_SCREENINGS`.

**FE đang dùng:** `audio` = `SUBTITLE`/`DUBBED`/`VOICEOVER`. FE sẽ suy từ `languageCode` + `subtitleCode`: có phụ đề là `SUBTITLE`, ngôn ngữ `vi` không phụ đề là `DUBBED`. "Thuyết minh" không phân biệt được với lồng tiếng: đề xuất `versionCode` chứa `-VO`.

### 4.4 Giá vé

| Method | Đường dẫn | Nội dung | Trạng thái |
| --- | --- | --- | --- |
| GET | `/price-rules?cinemaId=&roomTypeId=&seatTypeId=&status=&page=` | Danh sách | Có, kiểm tra đủ bộ lọc |
| POST | `/price-rules` | Tạo (`DRAFT`) | Có |
| PUT | `/price-rules/{id}` | Sửa. Chỉ sửa được `DRAFT`, `PENDING`. Muốn đổi quy tắc đã duyệt thì tạo quy tắc mới, cho bản cũ hết hạn | Mới |
| PATCH | `/price-rules/{id}/status` | Duyệt, ngừng | Có |
| GET | `/price-rules/preview?screeningId=&seatTypeId=` | Trả giá sẽ áp và quy tắc khớp. Dùng để kiểm tra trước khi duyệt | Mới, nên có |

Quyền: `CHAIN_ADMIN`. Thêm `CINEMA_MANAGER` nếu muốn quản lý rạp đặt giá riêng cho rạp mình.

Thứ tự khớp: lọc quy tắc `APPROVED` còn hiệu lực theo rạp (null = mọi rạp), loại phòng, loại ghế, `weekday_no` (null = mọi ngày; 1 = Thứ Hai … 7 = Chủ nhật), khung giờ (C1), rồi lấy `priority_no` **cao nhất**. Hai quy tắc cùng độ ưu tiên cùng khớp một suất là lỗi cấu hình: preview trả 409 `PRICE_RULE_AMBIGUOUS`.

**FE đang dùng:** `dayType` = `WEEKDAY`/`WEEKEND`/`HOLIDAY`. Mapping: `WEEKEND` thành hai quy tắc `weekday_no` 6 và 7, hoặc FE gửi danh sách ngày và BE tách. `HOLIDAY` theo C2. `timeFrom`/`timeTo` theo C1.

### 4.5 Đơn hàng

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/orders?cinemaId=&status=&channel=&date=&keyword=&page=` | `keyword` khớp mã đơn, SĐT, email khách |
| GET | `/orders/{id}` | Đơn, dòng đơn (vé, bắp nước), thanh toán, vé, khuyến mãi đã áp |
| POST | `/orders/{id}/confirm-payment` | Xác nhận đã thu tiền cho đơn bán tại quầy hoặc chuyển khoản. Ghi `payment_txn` `provider_code = 'MANUAL'` |

Quyền: `CHAIN_ADMIN`, `CINEMA_MANAGER`, `CASHIER` (chỉ xem và xác nhận đơn tại quầy).

```json
{
  "salesOrderId": 9001,
  "orderNumber": "TF-261113-88421",
  "orderType": "COMBINED",
  "salesChannel": "ONLINE",
  "cinemaId": 1,
  "cinemaName": "THEFILM Vincom Đồng Khởi",
  "customerName": "Trần Bảo Ngọc",
  "customerPhone": "0903123456",
  "filmTitle": "Tầng Mây Cuối Cùng",
  "showtimeAt": "2026-11-14T14:30:00+07:00",
  "seatLabels": ["F7", "F8"],
  "subtotalAmount": 400000,
  "discountAmount": 20000,
  "taxAmount": 0,
  "totalAmount": 380000,
  "orderStatus": "PAID",
  "createdAt": "2026-11-13T09:12:00+07:00",
  "paidAt": "2026-11-13T09:13:00+07:00"
}
```

`customerName` lấy theo thứ tự: hồ sơ thành viên, rồi `guest_name`. `filmTitle`, `showtimeAt`, `seatLabels` chỉ có khi đơn có vé. Đơn nhiều suất thì lấy suất sớm nhất và trả thêm `screeningCount`.

**Hoàn tiền không làm ở đây.** Nút "Hoàn tiền" trên trang Đơn hàng tạo yêu cầu ở 4.6. API **không** cho đổi `order_status` trực tiếp.

### 4.6 Hoàn và đổi vé

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/refund-requests?cinemaId=&status=&page=` | Danh sách yêu cầu hoàn |
| GET | `/refund-requests/{id}` | Kèm các dòng và giao dịch hoàn |
| POST | `/refund-requests` | Nhân viên tạo hộ khách: `{ salesOrderId, lines:[{orderLineId, amount}], reasonCode, reasonDetail }` |
| POST | `/refund-requests/{id}/approve` | `{ lines:[{refundRequestLineId, approvedAmount}] }`, cho phép duyệt một phần |
| POST | `/refund-requests/{id}/reject` | `{ reason }` |
| GET | `/ticket-changes?cinemaId=&status=&page=` | Danh sách yêu cầu đổi suất |
| POST | `/ticket-changes/{id}/approve`, `/reject` | Như trên |

Quyền: `CHAIN_ADMIN`, `CINEMA_MANAGER` trong phạm vi rạp của đơn.

Luật:

- Chỉ duyệt hoặc từ chối yêu cầu `PENDING`. Sai trạng thái trả 409 `REFUND_NOT_PENDING`.
- Duyệt thì: tạo `refund_txn` (idempotent theo `idempotency_key`), huỷ vé tương ứng, nhả `show_seat`, đảo `promo_redemption` và điểm thành viên đã tích. Tất cả trong một transaction. Gọi cổng thanh toán chạy nền qua `integration_outbox`. Trạng thái đi `APPROVED → PROCESSING → SUCCEEDED/FAILED`.
- `approvedAmount` ≤ `requestedAmount` (CHECK đã có). Phí hoàn lấy từ cài đặt `refund.feePct` (4.14).
- Yêu cầu gửi sau mốc `refund.beforeHours` vẫn được tạo nhưng bắt buộc duyệt tay. API trả cờ `outsidePolicy: true` để FE hiện cảnh báo.
- Đổi vé: `price_difference > 0` thì khách phải trả thêm trước khi duyệt; `< 0` thì tạo yêu cầu hoàn phần chênh.

```json
{
  "refundRequestId": 1,
  "requestNumber": "RF-0412",
  "salesOrderId": 8800,
  "orderNumber": "TF-261112-87110",
  "customerName": "Đỗ Minh Tuấn",
  "reasonCode": "SEAT_DEFECT",
  "reasonDetail": "Ghế E7 hỏng tay vịn, khách không xem được",
  "requestedAmount": 190000,
  "approvedAmount": null,
  "requestStatus": "PENDING",
  "outsidePolicy": false,
  "requestedAt": "2026-11-12T21:10:00+07:00"
}
```

**FE đang dùng:** một danh sách gộp hoàn và đổi (`kind` = `REFUND`/`CHANGE`), trạng thái chỉ ba giá trị. FE sẽ gọi hai endpoint rồi gộp, và hiện đủ các trạng thái `PROCESSING`, `SUCCEEDED`, `FAILED`.

### 4.7 Khuyến mãi và voucher

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/promo-campaigns?status=&keyword=&page=` | Kèm `usageCount` (đếm `promo_redemption` `APPLIED`) |
| POST | `/promo-campaigns` | Tạo (`DRAFT`) |
| PUT | `/promo-campaigns/{id}` | Sửa. Chiến dịch đã có lượt dùng thì không đổi được `discountType`, `discountValue`: 409 |
| PATCH | `/promo-campaigns/{id}/status` | `ACTIVE`, `PAUSED`, `CANCELLED` |
| GET | `/vouchers?campaignId=&status=&keyword=&page=` | Danh sách voucher |
| POST | `/promo-campaigns/{id}/vouchers` | Phát hành: `{ quantity, assignedUserIds?, validFrom, validTo }`. BE sinh mã |
| POST | `/vouchers/{id}/cancel` | Huỷ voucher chưa dùng |

Quyền: `CHAIN_ADMIN`.

Mã voucher do BE sinh, duy nhất, không đoán được. Không dùng `Math.random` như mock FE. Phát hành tối đa 10.000 mã mỗi lần. Phát hành nhiều hơn thì làm job nền và trả `202` kèm mã tác vụ.

**FE đang dùng:** trạng thái voucher `UNUSED`/`USED`/`EXPIRED`. FE sẽ đổi sang `ACTIVE`/`REDEEMED`/`EXPIRED`/`CANCELLED`. Chiến dịch có thêm `PENDING`, `EXPIRED`, `CANCELLED`, `maxDiscount`, `budgetAmount`, `perCustomerLimit`, `stackable`: FE sẽ bổ sung các trường này vào form.

### 4.8 Thành viên

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/members?tierId=&status=&keyword=&page=` | `keyword` khớp tên, SĐT, email, mã thành viên |
| GET | `/members/{id}` | Hồ sơ, hạng, điểm, đơn gần đây |
| PATCH | `/members/{id}/status` | `ACTIVE`, `SUSPENDED`, `CLOSED` |
| PATCH | `/members/{id}/tier` | Đổi hạng tay, bắt buộc `reason` |
| POST | `/members/{id}/points-adjustments` | `{ pointsDelta, reason }`, ghi `member_points_ledger` `entry_type = 'ADJUST'` |
| GET | `/points-ledger?memberId=&entryType=&page=` | Lịch sử điểm |
| GET | `/member-tiers` | Danh sách hạng |
| POST | `/member-tiers`, PUT `/member-tiers/{id}` | Tạo, sửa hạng |

Quyền: `CHAIN_ADMIN`; `SUPPORT` được xem và cộng/trừ điểm (C9).

```json
{
  "memberAccountId": 3,
  "memberNumber": "M000017",
  "fullName": "Nguyễn Thị Phương Thảo",
  "phone": "0988456789",
  "email": "phuongthao.nguyen.work@yahoo.com.vn",
  "tier": { "memberTierId": 3, "tierCode": "VVIP", "tierName": "VVIP" },
  "pointsBalance": 21450,
  "totalSpend": 12845000,
  "joinedAt": "2022-11-30T10:00:00+07:00",
  "status": "ACTIVE"
}
```

Luật:

- `pointsBalance` = tổng `points_delta` chưa hết hạn. **Không lưu cột số dư.** Tính trong truy vấn, hoặc dùng view nếu chậm.
- Trừ quá số dư trả 409 `POINTS_INSUFFICIENT`. FE đang chặn trước, BE vẫn phải chặn.
- `idempotency_key` của điều chỉnh tay = `X-Request-Id`, để bấm hai lần không cộng hai lần.
- `totalSpend` = tổng `total_amount` các đơn `PAID` trừ phần đã hoàn, trong 12 tháng gần nhất.

**Không có API tạo thành viên.** Thành viên do khách tự đăng ký ở cổng khách. FE sẽ bỏ nút "Thêm thành viên", trừ khi nghiệp vụ cần đăng ký hộ tại quầy; khi đó cần thêm `POST /members` tạo `app_user` không mật khẩu.

### 4.9 Bắp nước và kho

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET/POST | `/fnb/categories`, PUT `/fnb/categories/{id}` | Danh mục |
| GET | `/fnb/products?categoryId=&status=&keyword=&page=` | Sản phẩm kèm giá đang hiệu lực theo từng rạp |
| POST | `/fnb/products`, PUT `/fnb/products/{id}` | Sản phẩm. Combo gửi kèm `components:[{productId, quantity}]` |
| GET | `/fnb/products/{id}/prices` | Lịch sử giá theo rạp |
| POST | `/fnb/products/{id}/prices` | `{ cinemaIds:[], unitPrice, validFrom }`, đóng `valid_to` của giá cũ |
| GET | `/inventory/balances?cinemaId=&belowMin=&page=` | Tồn theo kho và sản phẩm |
| GET | `/inventory/txns?cinemaId=&txnType=&from=&to=&page=` | Phiếu kho |
| POST | `/inventory/txns` | Tạo phiếu `DRAFT` với nhiều dòng |
| POST | `/inventory/txns/{id}/post` | Ghi sổ: cập nhật `inventory_balance`, lưu `quantity_before/after` |
| POST | `/inventory/txns/{id}/reverse` | Đảo phiếu đã ghi sổ |

Quyền: `CHAIN_ADMIN` cho danh mục, sản phẩm, giá. `CINEMA_MANAGER` cho kho của rạp mình.

```json
{
  "inventoryLocationId": 3,
  "cinemaId": 3,
  "cinemaName": "THEFILM Royal City",
  "locationName": "Kho quầy",
  "fnbProductId": 40,
  "productName": "Ngô rang nguyên liệu",
  "unitCode": "kg",
  "onHandQuantity": 0,
  "reservedQuantity": 0,
  "minLevel": 30
}
```

Luật:

- Ghi sổ không được làm tồn âm, trừ phiếu `COUNT` (kiểm kê). Vi phạm trả 409 `INVENTORY_NEGATIVE`.
- Phiếu đã `POSTED` không sửa được, chỉ đảo.
- Bán bắp nước trừ kho qua phiếu `SALE`, do luồng bán hàng tạo, không qua API này.
- **Mức tồn tối thiểu (`minLevel`) chưa có cột.** Đề xuất lưu `app_setting` scope `CINEMA`, khoá `inventory.minLevel.{productId}`. Nếu cần sửa nhiều thì thêm migration cột `min_quantity` vào `inventory_balance`.

**FE đang dùng:** `sku` (= `product_code`), một giá duy nhất cho mỗi sản phẩm (đổi sang giá theo rạp), loại phiếu `IMPORT`/`EXPORT`/`ADJUST`. FE sẽ đổi loại phiếu sang `RECEIPT`/`ISSUE`/`ADJUSTMENT` và thêm `TRANSFER_*`, `COUNT`, `WASTE`. Phiếu mock FE chỉ có một dòng; API dùng nhiều dòng.

### 4.10 Hoá đơn và thuế

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/tax-invoices?legalEntityId=&status=&from=&to=&keyword=&page=` | `keyword` khớp mã hoá đơn, mã đơn, tên hoặc mã số thuế người mua |
| GET | `/tax-invoices/{id}` | Kèm các lần gửi `tax_provider_attempt` |
| POST | `/tax-invoices/{id}/retry` | Gửi lại hoá đơn `REJECTED` hoặc `TIMEOUT`: tạo attempt mới, `attempt_no + 1` |
| GET | `/tax-categories`, `/tax-rules` | Danh mục thuế, thuế suất |
| POST | `/tax-rules`, PATCH `/tax-rules/{id}/status` | Tạo, duyệt thuế suất |

Quyền: `CHAIN_ADMIN`, `ACCOUNTANT` (C9).

```json
{
  "taxInvoiceId": 2,
  "invoiceCode": "C26TTF-0001843",
  "salesOrderId": 8805,
  "orderNumber": "TF-261113-88502",
  "legalEntityCode": "TF-HCM",
  "buyerName": "Công ty TNHH Truyền thông Ánh Dương Việt Nam",
  "buyerTaxId": "0312345678",
  "totalAmount": 1284500,
  "taxAmount": 102760,
  "invoiceStatus": "REJECTED",
  "lastError": "Nhà cung cấp hoá đơn trả lỗi 503, đã thử 3 lần",
  "attemptCount": 3,
  "issuedAt": null,
  "createdAt": "2026-11-13T08:41:00+07:00"
}
```

Gửi lại phải idempotent theo `idempotency_key` của hoá đơn: nhà cung cấp nhận trùng thì không sinh hai hoá đơn. Hoá đơn `ACCEPTED` không gửi lại được: 409. `lastError` lấy từ attempt mới nhất.

**FE đang dùng:** trạng thái `ISSUED`/`PENDING`/`FAILED`. FE sẽ đổi sang `ACCEPTED`/`PENDING`/`REJECTED`/`TIMEOUT` (và `DRAFT`, `ADJUSTED`, `CANCELLED`). Banner "hoá đơn lỗi" đếm `REJECTED` + `TIMEOUT`.

### 4.11 Nhân sự: phân quyền, ca, chấm công, nghỉ phép, lương

Hồ sơ nhân viên và phân công đã có trong `admin-operations.md`. Phần dưới là phần còn thiếu.

#### 4.11.1 Phân quyền tài khoản

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/employees/{id}/access` | `{ appUserId, email, roles[], cinemaScopes[], enabled }` |
| PUT | `/employees/{id}/access/roles` | `{ roles: ["CINEMA_MANAGER"] }`, đồng bộ role sang Keycloak |
| POST | `/employees/{id}/access/disable`, `/enable` | Khoá, mở tài khoản đăng nhập (Keycloak `enabled`) |

Quyền: `CHAIN_ADMIN`. `HR` không được cấp role `CHAIN_ADMIN`.

Không được tự hạ quyền của chính mình. Không được khoá `CHAIN_ADMIN` cuối cùng của chuỗi: 409 `LAST_CHAIN_ADMIN`.

#### 4.11.2 Ca làm

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/shift-templates?cinemaId=` | Mẫu ca của rạp |
| POST | `/shift-templates`, PUT `/shift-templates/{id}` | `{ cinemaId, shiftCode, shiftName, startTime, endTime, unpaidBreakMinutes }` |
| GET | `/shift-assignments?cinemaId=&weekStart=` | Lịch một tuần (Thứ Hai → Chủ nhật) |
| PUT | `/shift-assignments/week` | **Lưu cả tuần một lần**: `{ cinemaId, weekStart, items:[{employeeId, date, shiftTemplateId}] }`. Ô không có trong `items` thì huỷ ca `PLANNED` cũ |
| POST | `/shift-assignments/week/publish` | Chuyển cả tuần `PLANNED → CONFIRMED`, gửi thông báo qua `integration_outbox` |

Quyền: `CHAIN_ADMIN`, `HR`, `CINEMA_MANAGER` (rạp mình).

Luật:

- Ca qua nửa đêm (`18:00–00:30`): `ends_at` là ngày hôm sau.
- Một nhân viên không có hai ca chồng giờ, kể cả ở hai rạp: 409 `SHIFT_OVERLAP`, kèm danh sách ô lỗi để FE tô đỏ.
- Nhân viên phải có phân công còn hiệu lực ở rạp đó trong ngày đó.
- Ngày đã có đơn nghỉ được duyệt thì không xếp ca được.
- Ca `COMPLETED` không sửa, không huỷ được.

#### 4.11.3 Chấm công và nghỉ phép

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/attendance?cinemaId=&date=&employeeId=&page=` | Kèm ca, giờ vào/ra, `lateMinutes`, `earlyLeaveMinutes` (tính, không lưu) |
| POST | `/attendance/{id}/approve` | Duyệt công |
| GET | `/attendance-changes?status=` | Yêu cầu sửa công |
| POST | `/attendance-changes/{id}/approve`, `/reject` | Duyệt thì cập nhật `attendance_record`, trạng thái `CORRECTED` |
| GET | `/leave-types`, POST, PUT | Loại nghỉ |
| GET | `/leave-requests?cinemaId=&status=&page=` | Đơn nghỉ |
| POST | `/leave-requests/{id}/approve`, `/reject` | Duyệt thì huỷ các ca `PLANNED` trong khoảng nghỉ và trả về danh sách ca bị huỷ |

Quyền: `CHAIN_ADMIN`, `HR`, `CINEMA_MANAGER` (rạp mình).

Chấm công vào/ra do app nhân viên hoặc máy chấm công gửi lên, không thuộc phạm vi tài liệu này.

#### 4.11.4 Bảng lương

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/payroll/periods?chainId=` | Kỳ lương |
| POST | `/payroll/periods` | Mở kỳ: `{ periodCode: "2026-11", periodStart, periodEnd }` |
| POST | `/payroll/periods/{id}/runs` | Tính lương: tạo `payroll_run` `DRAFT` từ chấm công đã duyệt, thuế TNCN theo `pit_rule_set` |
| GET | `/payroll/runs/{id}` | Tổng: `headcount`, `grossAmount`, `pitAmount`, `netAmount`, kèm từng nhân viên |
| POST | `/payroll/runs/{id}/submit`, `/approve`, `/mark-paid` | `DRAFT → REVIEW → APPROVED → PAID` |

Quyền: `CHAIN_ADMIN`, `HR` tính lương; `ACCOUNTANT` duyệt và đánh dấu đã trả (C9).

Mỗi kỳ chỉ có một run `APPROVED` hoặc `PAID`. Tính lại thì huỷ run `DRAFT` cũ. Run đã `APPROVED` không sửa được. Lương là dữ liệu nhạy cảm: **không trả lương từng người** cho `CINEMA_MANAGER`.

**FE đang dùng:** lịch ca chỉ lưu tên ca và thứ trong tuần; mẫu ca dùng chung toàn chuỗi (schema theo rạp); bảng lương một dòng mỗi kỳ, trạng thái `DRAFT`/`LOCKED`/`PAID`. FE sẽ đổi theo schema (`LOCKED` thành `APPROVED`), thêm bước `REVIEW`, và gọi API lưu cả tuần thay vì lưu từng ô.

### 4.12 CSKH

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/support-cases?cinemaId=&status=&priority=&assignedTo=&keyword=&page=` | Danh sách yêu cầu |
| GET | `/support-cases/{id}` | Kèm đơn liên quan và các hội thoại |
| PATCH | `/support-cases/{id}/status` | `{ status, note }`. Chuyển trạng thái theo luồng ở dưới |
| PATCH | `/support-cases/{id}/assignee` | `{ assignedTo }` |
| GET | `/support-cases/{id}/messages` | Tin nhắn mọi hội thoại, cũ nhất trước |
| POST | `/support-cases/{id}/messages` | `{ body }`, `sender_type = 'AGENT'`. Gửi tới khách qua kênh của hội thoại (outbox) |
| GET | `/knowledge-articles?topic=&status=&keyword=&page=` | Bài hướng dẫn, kèm `topic` (C8) |
| POST | `/knowledge-articles`, PUT `/knowledge-articles/{id}` | Soạn, sửa |
| POST | `/knowledge-articles/{id}/submit`, `/approve`, `/retire` | `DRAFT → PENDING → APPROVED → RETIRED` |

Quyền: `CHAIN_ADMIN`, `SUPPORT` (C9); `CINEMA_MANAGER` xem yêu cầu của rạp mình.

Luồng trạng thái yêu cầu: `NEW → AI_HANDLING → WAITING_AGENT → IN_PROGRESS → RESOLVED → CLOSED`. Từ `RESOLVED` mở lại được về `IN_PROGRESS` (tăng `reopened_count`). `CLOSED` là kết thúc.

Tin nhắn có thể chứa dữ liệu cá nhân. Tin đã `redacted_at` thì trả `body: null`, `redacted: true`.

**FE đang dùng:** trạng thái `OPEN`/`IN_PROGRESS`/`WAITING`/`RESOLVED`/`CLOSED`, ưu tiên `LOW`/`MEDIUM`/`HIGH`, kênh `WEB`/`HOTLINE`/`EMAIL`/`CHAT` ở cấp yêu cầu. FE sẽ đổi theo schema: ưu tiên `NORMAL`/`URGENT`; kênh lấy từ hội thoại và dùng mã `PHONE`, `MOBILE`, `SOCIAL`. Nút "Chuyển bước" thành chọn trạng thái kế tiếp hợp lệ.

### 4.13 Tổng quan và báo cáo

Các endpoint chỉ đọc, tổng hợp từ `sales_order`, `sales_order_line`, `ticket`, `show_seat`, `screening`, `refund_txn`. Mọi endpoint nhận `cinemaId` (bỏ trống = toàn bộ phạm vi người gọi), `from`, `to` (ngày, gồm cả hai đầu). Số liệu tính theo giờ Việt Nam.

| Method | Đường dẫn | Dùng cho |
| --- | --- | --- |
| GET | `/dashboard/summary?cinemaId=&range=today\|7d\|30d` | 4 ô KPI ở Tổng quan, kèm phần trăm so với kỳ liền trước cùng độ dài |
| GET | `/dashboard/occupancy?cinemaId=&date=` | Lấp đầy theo rạp trong ngày |
| GET | `/dashboard/attention?cinemaId=` | Khối "Cần can thiệp" |
| GET | `/reports/revenue-daily?cinemaId=&from=&to=` | Biểu đồ doanh thu theo ngày |
| GET | `/reports/by-film?…` | Bảng theo phim |
| GET | `/reports/by-cinema?…` | Bảng theo rạp |
| GET | `/reports/fnb-top?…&limit=10` | Bắp nước bán chạy |
| GET | `/reports/export?type=by-film\|by-cinema\|…&format=xlsx` | Tải file, trả `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |

Quyền: `CHAIN_ADMIN`, `CINEMA_MANAGER` (rạp mình), `ACCOUNTANT`.

`/dashboard/summary`:

```json
{
  "range": "today",
  "revenue": 186450000,
  "revenueChangePct": 12.4,
  "ticketsSold": 1248,
  "ticketsChangePct": 8.1,
  "occupancyPct": 78,
  "seatsSold": 1248,
  "seatsTotal": 1600,
  "nowShowingFilms": 4,
  "openOrders": 36,
  "openSupportCases": 3
}
```

`/dashboard/attention` trả danh sách đã xếp theo mức độ, mỗi mục có `type`, `level`, `refId`, `title`, `detail`:

| `type` | Khi nào | `level` |
| --- | --- | --- |
| `SLOW_SALES` | Suất `ON_SALE` trong 3 giờ tới mà bán dưới 20% ghế | `high` |
| `SOLD_OUT` | Suất hôm nay đã hết vé | `info` |
| `ROOM_MAINTENANCE` | Phòng `MAINTENANCE` còn suất chưa huỷ | `mid` |
| `SEAT_MAINTENANCE` | Ghế bảo trì giao với suất đã bán vé | `mid` |
| `SUPPORT_URGENT` | Yêu cầu CSKH `HIGH`/`URGENT` chưa xử lý | `high` |
| `PENDING_PAYMENT` | Đơn chờ thanh toán quá thời gian giữ ghế | `mid` |
| `REFUND_PENDING` | Yêu cầu hoàn hoặc đổi chờ duyệt | `mid` |
| `INVOICE_FAILED` | Hoá đơn `REJECTED`/`TIMEOUT` | `mid` |
| `LOW_STOCK` | Tồn dưới mức tối thiểu | `info` |

Ngưỡng (20%, 3 giờ) đọc từ `app_setting`, không viết cứng.

`/reports/by-film` trả thêm `distributorShare` = doanh thu vé × `revenue_share_pct` (C5). Doanh thu luôn là **thực thu**: trừ giảm giá, trừ phần đã hoàn, chưa gồm thuế. Mỗi response ghi rõ `"basis": "NET_EXCL_TAX"` để FE và kế toán đối chiếu.

Báo cáo trên 31 ngày có thể chậm. Đề xuất bảng tổng hợp theo ngày, do job đêm cập nhật. Đây là bảng mới: nếu làm thì cần migration, ghi lại ở đây.

### 4.14 Nhật ký thao tác và cài đặt

| Method | Đường dẫn | Nội dung |
| --- | --- | --- |
| GET | `/audit-events?cinemaId=&actorUserId=&actionCode=&objectType=&from=&to=&page=` | Mới nhất trước. Trả `actorName` (join `app_user`/`employee`); `beforeData`, `afterData` dạng JSON |
| GET | `/settings?scopeType=GLOBAL&scopeId=` | Các khoá đang hiệu lực |
| PUT | `/settings` | `{ items:[{ key, value }] }`. Tạo bản ghi mới có `valid_from = now`, đóng `valid_to` bản cũ. Không ghi đè |

Quyền: `CHAIN_ADMIN`. Nhật ký thao tác: `CINEMA_MANAGER` chỉ xem sự kiện thuộc rạp mình.

Nhật ký trả về **phải che dữ liệu nhạy cảm** trong `beforeData`/`afterData`: mật khẩu, token, QR, số thẻ, lương.

Khoá cài đặt dùng cho trang Cài đặt (`scope_type = 'GLOBAL'`, `scope_id = '*'`):

| Khoá | Kiểu | Mặc định | Dùng ở |
| --- | --- | --- | --- |
| `booking.holdMinutes` | NUMBER | 10 | Giữ ghế khi thanh toán. Đang viết cứng `plusMinutes(10)` trong `BookingServiceImpl`: chuyển sang đọc khoá này |
| `booking.maxTicketsPerOrder` | NUMBER | 8 | Đặt vé |
| `booking.saleOpenDays` | NUMBER | 14 | Mở bán suất |
| `refund.beforeHours` | NUMBER | 4 | 4.6 |
| `refund.feePct` | NUMBER | 10 | 4.6 |
| `ticketChange.beforeHours` | NUMBER | 2 | 4.6 |
| `payment.vnpay.enabled`, `payment.momo.enabled`, `payment.card.enabled` | BOOLEAN | true, true, false | Bước thanh toán |
| `invoice.provider` | STRING | `MOCK` | 4.10 (C10) |
| `invoice.autoIssue` | BOOLEAN | true | 4.10 |
| `notify.email.enabled`, `notify.sms.enabled` | BOOLEAN | true, false | Gửi vé |
| `dashboard.slowSalesPct`, `dashboard.slowSalesWithinHours` | NUMBER | 20, 3 | 4.13 |

Sai kiểu (`"abc"` cho NUMBER) hoặc ngoài khoảng (giữ ghế < 1 phút) trả 400, kèm `error.code = SETTING_INVALID` và khoá sai.

---

## 5. Thứ tự làm đề xuất

Ưu tiên phần mở khoá vận hành và phần dính tới tiền:

1. **Chốt mục 2**, làm migration cho C1, C5, C6 nếu chọn.
2. **Bổ sung API đã có một phần** (4.1–4.4). Việc nhỏ, giúp FE bỏ mock cho cả nhóm Vận hành.
3. **Đơn hàng, Hoàn & đổi vé** (4.5, 4.6). Đây là chỗ dính tiền, cần trước khi chạy thật.
4. **Tổng quan và báo cáo** (4.13).
5. **Khuyến mãi, Thành viên, Hoá đơn, CSKH** (4.7, 4.8, 4.10, 4.12).
6. **Bắp nước và kho** (4.9), **Nhân sự** (4.11), **Nhật ký, Cài đặt** (4.14).

## 6. Phía FE nối API thế nào

Để hai bên biết ai đổi gì:

- Dữ liệu các trang mới đều đi qua `FE/libs/admin/data-access/src/lib/services/admin-backoffice.service.ts`. Mỗi thuộc tính (`priceRules`, `members`…) là một `MockCollection`. Khi có API, FE thay thuộc tính đó bằng lời gọi `HttpClient` và giữ tên. Component không phải sửa.
- Model FE ở `backoffice.models.ts` sẽ đổi tên trường, tên trạng thái cho khớp response trong tài liệu này. Mục **FE đang dùng** của từng module là danh sách việc FE phải đổi.
- `AdminOpsService` (dashboard, đơn hàng, khuyến mãi, CSKH) và `AdminCatalogService` (phim, rạp, suất) hiện cũng là mock. Hai service này nối theo 4.1–4.5, 4.7, 4.12, 4.13.
- Khi API trả `error.message`, FE hiện nguyên câu đó, nên câu phải viết cho người dùng cuối đọc được, không lộ tên bảng hay stack trace.
