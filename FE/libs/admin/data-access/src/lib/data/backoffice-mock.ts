import {
  AppSettings,
  AttendanceRecord,
  AuditEvent,
  Employee,
  FilmVersion,
  FnbCategory,
  FnbProduct,
  InventoryBalance,
  InventoryTxn,
  KnowledgeArticle,
  LeaveRequest,
  LegalEntity,
  Member,
  MemberTier,
  OrgChain,
  PayrollRun,
  PointsTxn,
  PriceRule,
  PromoVoucher,
  RefundRequest,
  ReleaseRight,
  RoomType,
  SeatMaintenance,
  SeatType,
  ShiftAssignment,
  ShiftTemplate,
  SupportMessage,
  TaxInvoice,
  TaxRule,
} from '../models/backoffice.models';

// Dữ liệu giả, khớp với admin-mock.ts (cùng tên rạp, phim, mã đơn, khách).

export const MOCK_CHAINS: OrgChain[] = [
  { id: 1, code: 'THEFILM', name: 'Chuỗi rạp THEFILM', hotline: '1900 6868', website: 'thefilm.vn', status: 'ACTIVE' },
];

export const MOCK_LEGAL_ENTITIES: LegalEntity[] = [
  { id: 1, code: 'TF-HCM', name: 'Công ty TNHH Giải trí THEFILM Sài Gòn', chainName: 'Chuỗi rạp THEFILM', taxCode: '0316789012', address: '72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh', representative: 'Nguyễn Minh Quân', status: 'ACTIVE' },
  { id: 2, code: 'TF-HN', name: 'Công ty Cổ phần Điện ảnh THEFILM Hà Nội', chainName: 'Chuỗi rạp THEFILM', taxCode: '0109876543', address: '72A Nguyễn Trãi, Thượng Đình, Thanh Xuân, Hà Nội', representative: 'Trần Thị Phương Thảo', status: 'ACTIVE' },
  { id: 3, code: 'TF-DN', name: 'Chi nhánh Công ty TNHH Giải trí THEFILM Sài Gòn tại Đà Nẵng', chainName: 'Chuỗi rạp THEFILM', taxCode: '0316789012-001', address: '910 Ngô Quyền, An Hải Bắc, Sơn Trà, Đà Nẵng', representative: 'Lê Văn Hùng', status: 'INACTIVE' },
];

export const MOCK_ROOM_TYPES: RoomType[] = [
  { id: 1, code: 'STANDARD', name: 'Tiêu chuẩn', description: 'Màn hình 2D/3D, âm thanh 7.1', surcharge: 0, status: 'ACTIVE' },
  { id: 2, code: 'IMAX', name: 'IMAX', description: 'Màn hình IMAX Laser, âm thanh 12 kênh', surcharge: 60000, status: 'ACTIVE' },
  { id: 3, code: '4DX', name: '4DX', description: 'Ghế chuyển động, hiệu ứng gió, nước, mùi', surcharge: 90000, status: 'ACTIVE' },
  { id: 4, code: 'GOLD', name: 'Gold Class', description: 'Ghế ngả, phục vụ đồ ăn tại chỗ', surcharge: 150000, status: 'ACTIVE' },
  { id: 5, code: 'ATMOS', name: 'Dolby Atmos', description: 'Âm thanh vòm Dolby Atmos', surcharge: 20000, status: 'INACTIVE' },
];

export const MOCK_SEAT_TYPES: SeatType[] = [
  { id: 1, code: 'STANDARD', name: 'Ghế thường', seatsPerUnit: 1, surcharge: 0, status: 'ACTIVE' },
  { id: 2, code: 'VIP', name: 'Ghế VIP', seatsPerUnit: 1, surcharge: 15000, status: 'ACTIVE' },
  { id: 3, code: 'COUPLE', name: 'Ghế đôi Sweetbox', seatsPerUnit: 2, surcharge: 40000, status: 'ACTIVE' },
];

export const MOCK_SEAT_MAINTENANCE: SeatMaintenance[] = [
  { id: 1, roomId: 1, cinemaName: 'THEFILM Vincom Đồng Khởi', roomName: 'IMAX 1', seatLabel: 'E7', reason: 'Tay vịn gãy', reportedAt: '2026-11-12T19:30:00+07:00', status: 'OPEN' },
  { id: 2, roomId: 1, cinemaName: 'THEFILM Vincom Đồng Khởi', roomName: 'IMAX 1', seatLabel: 'E8', reason: 'Đệm ngồi rách', reportedAt: '2026-11-12T19:31:00+07:00', status: 'OPEN' },
  { id: 3, roomId: 8, cinemaName: 'THEFILM Indochina Đà Nẵng', roomName: 'Standard 1', seatLabel: 'Toàn phòng', reason: 'Thay hệ thống điều hoà, dự kiến xong 16/11', reportedAt: '2026-11-10T08:00:00+07:00', status: 'OPEN' },
  { id: 4, roomId: 4, cinemaName: 'THEFILM Aeon Tân Bình', roomName: '4DX 1', seatLabel: 'C3', reason: 'Bộ rung không hoạt động', reportedAt: '2026-11-05T15:00:00+07:00', status: 'RESOLVED' },
];

export const MOCK_FILM_VERSIONS: FilmVersion[] = [
  { id: 1, filmTitle: 'Tầng Mây Cuối Cùng', format: 'IMAX', audio: 'SUBTITLE', language: 'Tiếng Anh', durationMin: 136, status: 'ACTIVE' },
  { id: 2, filmTitle: 'Tầng Mây Cuối Cùng', format: '2D', audio: 'SUBTITLE', language: 'Tiếng Anh', durationMin: 136, status: 'ACTIVE' },
  { id: 3, filmTitle: 'Tầng Mây Cuối Cùng', format: '2D', audio: 'DUBBED', language: 'Tiếng Việt', durationMin: 136, status: 'ACTIVE' },
  { id: 4, filmTitle: 'Mùa Hè Bất Tận', format: '4DX', audio: 'SUBTITLE', language: 'Tiếng Hàn', durationMin: 118, status: 'ACTIVE' },
  { id: 5, filmTitle: 'Mùa Hè Bất Tận', format: '2D', audio: 'SUBTITLE', language: 'Tiếng Hàn', durationMin: 118, status: 'ACTIVE' },
  { id: 6, filmTitle: 'Khu Rừng Kỳ Diệu', format: '3D', audio: 'DUBBED', language: 'Tiếng Việt', durationMin: 108, status: 'ACTIVE' },
  { id: 7, filmTitle: 'Hồ Sơ Đêm Mưa', format: '2D', audio: 'SUBTITLE', language: 'Tiếng Việt', durationMin: 125, status: 'INACTIVE' },
];

export const MOCK_RELEASE_RIGHTS: ReleaseRight[] = [
  { id: 1, filmTitle: 'Tầng Mây Cuối Cùng', distributor: 'CJ HK Entertainment', cinemaScope: 'Toàn chuỗi', startDate: '2026-10-25', endDate: '2026-12-25', revenueSharePct: 50, status: 'ACTIVE' },
  { id: 2, filmTitle: 'Mùa Hè Bất Tận', distributor: 'Lotte Entertainment Việt Nam', cinemaScope: 'Toàn chuỗi', startDate: '2026-10-28', endDate: '2026-12-15', revenueSharePct: 48, status: 'ACTIVE' },
  { id: 3, filmTitle: 'Khu Rừng Kỳ Diệu', distributor: 'Galaxy Studio', cinemaScope: 'Toàn chuỗi', startDate: '2026-11-14', endDate: '2027-01-14', revenueSharePct: 45, status: 'ACTIVE' },
  { id: 4, filmTitle: 'Hồ Sơ Đêm Mưa', distributor: 'Beta Media', cinemaScope: 'THEFILM Vincom Đồng Khởi', startDate: '2026-11-15', endDate: '2026-12-31', revenueSharePct: 55, status: 'PENDING' },
  { id: 5, filmTitle: 'Đêm Trắng Sài Gòn', distributor: 'BHD Star Distribution', cinemaScope: 'Toàn chuỗi', startDate: '2026-09-01', endDate: '2026-10-31', revenueSharePct: 50, status: 'EXPIRED' },
];

export const MOCK_PRICE_RULES: PriceRule[] = [
  { id: 1, name: 'Ngày thường trước 17h', cinemaScope: 'Toàn chuỗi', roomType: 'STANDARD', seatType: 'STANDARD', dayType: 'WEEKDAY', timeFrom: '08:00', timeTo: '17:00', price: 75000, priority: 10, status: 'ACTIVE' },
  { id: 2, name: 'Ngày thường sau 17h', cinemaScope: 'Toàn chuỗi', roomType: 'STANDARD', seatType: 'STANDARD', dayType: 'WEEKDAY', timeFrom: '17:00', timeTo: '23:59', price: 95000, priority: 10, status: 'ACTIVE' },
  { id: 3, name: 'Cuối tuần cả ngày', cinemaScope: 'Toàn chuỗi', roomType: 'STANDARD', seatType: 'STANDARD', dayType: 'WEEKEND', timeFrom: '08:00', timeTo: '23:59', price: 110000, priority: 20, status: 'ACTIVE' },
  { id: 4, name: 'IMAX cuối tuần', cinemaScope: 'Toàn chuỗi', roomType: 'IMAX', seatType: 'STANDARD', dayType: 'WEEKEND', timeFrom: '08:00', timeTo: '23:59', price: 190000, priority: 30, status: 'ACTIVE' },
  { id: 5, name: 'Gold Class Vincom Đồng Khởi', cinemaScope: 'THEFILM Vincom Đồng Khởi', roomType: 'GOLD', seatType: 'VIP', dayType: 'WEEKEND', timeFrom: '17:00', timeTo: '23:59', price: 350000, priority: 40, status: 'ACTIVE' },
  { id: 6, name: 'Tết Dương lịch 2027', cinemaScope: 'Toàn chuỗi', roomType: 'STANDARD', seatType: 'STANDARD', dayType: 'HOLIDAY', timeFrom: '08:00', timeTo: '23:59', price: 130000, priority: 50, status: 'INACTIVE' },
];

export const MOCK_REFUND_REQUESTS: RefundRequest[] = [
  { id: 1, code: 'RF-0412', orderCode: 'TF-261112-87110', customerName: 'Đỗ Minh Tuấn', kind: 'REFUND', reason: 'Ghế E7 hỏng tay vịn, khách không xem được', amount: 190000, createdAt: '2026-11-12T21:10:00+07:00', status: 'PENDING' },
  { id: 2, code: 'CH-0198', orderCode: 'TF-261113-88455', customerName: 'Lê Hoàng Nam', kind: 'CHANGE', reason: 'Đổi sang suất 21:30 cùng ngày', amount: 0, createdAt: '2026-11-13T10:20:00+07:00', status: 'PENDING' },
  { id: 3, code: 'RF-0409', orderCode: 'TF-261113-88502', customerName: 'Nguyễn Thị Phương Thảo', kind: 'REFUND', reason: 'Đặt nhầm rạp, báo trước giờ chiếu 6 tiếng', amount: 1284500, createdAt: '2026-11-11T09:00:00+07:00', status: 'APPROVED' },
  { id: 4, code: 'RF-0405', orderCode: 'TF-261113-88601', customerName: 'Võ Anh Thư', kind: 'REFUND', reason: 'Báo sau giờ chiếu', amount: 95000, createdAt: '2026-11-10T23:15:00+07:00', status: 'REJECTED' },
];

export const MOCK_VOUCHERS: PromoVoucher[] = [
  { id: 1, code: 'TFU20-8K2M', campaign: 'Giảm 20% suất sớm tuần', issuedTo: 'Trần Bảo Ngọc', expiresAt: '2026-11-30', status: 'UNUSED' },
  { id: 2, code: 'TFU20-Q9PL', campaign: 'Giảm 20% suất sớm tuần', issuedTo: 'Lê Hoàng Nam', expiresAt: '2026-11-30', status: 'USED' },
  { id: 3, code: 'MEMBER50-77AC', campaign: 'Thành viên mới giảm 50K', issuedTo: 'Phạm Thu Hà', expiresAt: '2026-10-31', status: 'EXPIRED' },
  { id: 4, code: 'COMBO30K-H3D1', campaign: 'Giảm 30K combo bắp nước', issuedTo: 'Không chỉ định', expiresAt: '2026-12-10', status: 'UNUSED' },
];

export const MOCK_MEMBER_TIERS: MemberTier[] = [
  { id: 1, code: 'MEMBER', name: 'Thành viên', minSpend: 0, pointRatePct: 5, perks: 'Tích 5% giá trị đơn', status: 'ACTIVE' },
  { id: 2, code: 'VIP', name: 'VIP', minSpend: 4000000, pointRatePct: 7, perks: '1 vé 2D miễn phí mỗi tháng sinh nhật', status: 'ACTIVE' },
  { id: 3, code: 'VVIP', name: 'VVIP', minSpend: 8000000, pointRatePct: 10, perks: 'Combo miễn phí, ưu tiên suất chiếu sớm', status: 'ACTIVE' },
];

export const MOCK_MEMBERS: Member[] = [
  { id: 1, code: 'M000128', name: 'Trần Bảo Ngọc', phone: '0903 123 456', email: 'ngoc.tran@gmail.com', tier: 'VIP', points: 4820, totalSpend: 5120000, joinedAt: '2024-03-12', status: 'ACTIVE' },
  { id: 2, code: 'M000342', name: 'Lê Hoàng Nam', phone: '0912 888 012', email: 'nam.le@outlook.com', tier: 'Thành viên', points: 640, totalSpend: 860000, joinedAt: '2025-08-02', status: 'ACTIVE' },
  { id: 3, code: 'M000017', name: 'Nguyễn Thị Phương Thảo', phone: '0988 456 789', email: 'phuongthao.nguyen.work@yahoo.com.vn', tier: 'VVIP', points: 21450, totalSpend: 12845000, joinedAt: '2022-11-30', status: 'ACTIVE' },
  { id: 4, code: 'M000501', name: 'Võ Anh Thư', phone: '0971 222 333', email: 'anhthu.vo@gmail.com', tier: 'Thành viên', points: 0, totalSpend: 0, joinedAt: '2026-11-13', status: 'ACTIVE' },
  { id: 5, code: 'M000233', name: 'Đỗ Minh Tuấn', phone: '0935 111 999', email: 'tuan.do@gmail.com', tier: 'Thành viên', points: 1210, totalSpend: 1450000, joinedAt: '2025-01-20', status: 'INACTIVE' },
];

export const MOCK_POINTS_LEDGER: PointsTxn[] = [
  { id: 1, memberName: 'Trần Bảo Ngọc', delta: 380, reason: 'Tích điểm đơn TF-261113-88421', at: '2026-11-13T09:15:00+07:00' },
  { id: 2, memberName: 'Nguyễn Thị Phương Thảo', delta: -2000, reason: 'Đổi combo bắp nước', at: '2026-11-12T20:02:00+07:00' },
  { id: 3, memberName: 'Lê Hoàng Nam', delta: 120, reason: 'Bù điểm do lỗi thanh toán (CSKH CS-88455)', at: '2026-11-11T14:40:00+07:00' },
];

export const MOCK_FNB_CATEGORIES: FnbCategory[] = [
  { id: 1, name: 'Bắp rang', sortOrder: 1, status: 'ACTIVE' },
  { id: 2, name: 'Nước uống', sortOrder: 2, status: 'ACTIVE' },
  { id: 3, name: 'Combo', sortOrder: 3, status: 'ACTIVE' },
  { id: 4, name: 'Đồ ăn nhẹ', sortOrder: 4, status: 'INACTIVE' },
];

export const MOCK_FNB_PRODUCTS: FnbProduct[] = [
  { id: 1, sku: 'POP-SAL-L', name: 'Bắp rang muối size L', category: 'Bắp rang', price: 69000, cinemaScope: 'Toàn chuỗi', status: 'ACTIVE' },
  { id: 2, sku: 'POP-CAR-L', name: 'Bắp rang caramel size L', category: 'Bắp rang', price: 79000, cinemaScope: 'Toàn chuỗi', status: 'ACTIVE' },
  { id: 3, sku: 'DRK-COK-M', name: 'Coca-Cola 22oz', category: 'Nước uống', price: 39000, cinemaScope: 'Toàn chuỗi', status: 'ACTIVE' },
  { id: 4, sku: 'CMB-COUPLE', name: 'Combo Đôi (1 bắp L caramel phô mai + 2 nước 22oz + 1 snack khoai tây)', category: 'Combo', price: 159000, cinemaScope: 'Toàn chuỗi', status: 'ACTIVE' },
  { id: 5, sku: 'CMB-GOLD', name: 'Combo Gold Class', category: 'Combo', price: 289000, cinemaScope: 'THEFILM Vincom Đồng Khởi', status: 'ACTIVE' },
  { id: 6, sku: 'SNK-NACHO', name: 'Nachos phô mai', category: 'Đồ ăn nhẹ', price: 65000, cinemaScope: 'Toàn chuỗi', status: 'INACTIVE' },
];

export const MOCK_INVENTORY: InventoryBalance[] = [
  { id: 1, cinemaName: 'THEFILM Vincom Đồng Khởi', item: 'Ngô rang nguyên liệu', unit: 'kg', onHand: 42, minLevel: 30 },
  { id: 2, cinemaName: 'THEFILM Vincom Đồng Khởi', item: 'Ly giấy 22oz', unit: 'cái', onHand: 180, minLevel: 500 },
  { id: 3, cinemaName: 'THEFILM Royal City', item: 'Ngô rang nguyên liệu', unit: 'kg', onHand: 0, minLevel: 30 },
  { id: 4, cinemaName: 'THEFILM Royal City', item: 'Siro Coca-Cola', unit: 'bình', onHand: 12, minLevel: 6 },
  { id: 5, cinemaName: 'THEFILM Aeon Tân Bình', item: 'Đường caramel', unit: 'kg', onHand: 25, minLevel: 10 },
];

export const MOCK_INVENTORY_TXNS: InventoryTxn[] = [
  { id: 1, code: 'NK-261113-01', cinemaName: 'THEFILM Vincom Đồng Khởi', kind: 'IMPORT', item: 'Ngô rang nguyên liệu', qty: 50, at: '2026-11-13T08:00:00+07:00', by: 'Phạm Văn Lộc' },
  { id: 2, cinemaName: 'THEFILM Royal City', code: 'XK-261113-04', kind: 'EXPORT', item: 'Ngô rang nguyên liệu', qty: 18, at: '2026-11-13T21:40:00+07:00', by: 'Hoàng Thị Mai' },
  { id: 3, code: 'DC-261112-01', cinemaName: 'THEFILM Aeon Tân Bình', kind: 'ADJUST', item: 'Đường caramel', qty: -2, at: '2026-11-12T22:10:00+07:00', by: 'Ngô Đức Anh' },
];

export const MOCK_TAX_INVOICES: TaxInvoice[] = [
  { id: 1, number: 'C26TTF-0001842', orderCode: 'TF-261113-88421', legalEntity: 'TF-HCM', buyer: 'Trần Bảo Ngọc', amount: 380000, vat: 30400, issuedAt: '2026-11-13T09:13:00+07:00', status: 'ISSUED' },
  { id: 2, number: 'C26TTF-0001843', orderCode: 'TF-261113-88502', legalEntity: 'TF-HCM', buyer: 'Công ty TNHH Truyền thông Ánh Dương Việt Nam', amount: 1284500, vat: 102760, issuedAt: '2026-11-13T08:41:00+07:00', status: 'FAILED', error: 'Nhà cung cấp hoá đơn trả lỗi 503, đã thử 3 lần' },
  { id: 3, number: '—', orderCode: 'TF-261113-88455', legalEntity: 'TF-HN', buyer: 'Lê Hoàng Nam', amount: 190000, vat: 15200, issuedAt: '2026-11-13T10:06:00+07:00', status: 'PENDING' },
];

export const MOCK_TAX_RULES: TaxRule[] = [
  { id: 1, category: 'Vé xem phim', ratePct: 5, effectiveFrom: '2026-01-01', note: 'Dịch vụ chiếu phim' },
  { id: 2, category: 'Bắp nước', ratePct: 8, effectiveFrom: '2026-07-01', note: 'Áp dụng giảm thuế GTGT theo nghị định hiện hành' },
  { id: 3, category: 'Quảng cáo màn ảnh', ratePct: 10, effectiveFrom: '2026-01-01', note: '' },
];

export const MOCK_EMPLOYEES: Employee[] = [
  { id: 1, code: 'NV0001', name: 'Nguyễn Minh Quân', department: 'Ban điều hành', position: 'Giám đốc vận hành', cinema: 'Văn phòng chuỗi', phone: '0909 000 111', account: 'quan.admin@thefilm.vn', role: 'CHAIN_ADMIN', scope: 'Toàn chuỗi', status: 'ACTIVE' },
  { id: 2, code: 'NV0014', name: 'Trần Thị Phương Thảo', department: 'Vận hành rạp', position: 'Quản lý rạp', cinema: 'THEFILM Royal City', phone: '0912 345 678', account: 'thao.tran@thefilm.vn', role: 'CINEMA_MANAGER', scope: 'THEFILM Royal City', status: 'ACTIVE' },
  { id: 3, code: 'NV0021', name: 'Phạm Văn Lộc', department: 'Vận hành rạp', position: 'Quản lý rạp', cinema: 'THEFILM Vincom Đồng Khởi', phone: '0903 777 222', account: 'loc.pham@thefilm.vn', role: 'CINEMA_MANAGER', scope: 'THEFILM Vincom Đồng Khởi', status: 'ACTIVE' },
  { id: 4, code: 'NV0102', name: 'Hoàng Thị Mai', department: 'Quầy vé & bắp nước', position: 'Nhân viên quầy', cinema: 'THEFILM Royal City', phone: '0987 654 321', account: '', role: 'STAFF', scope: 'THEFILM Royal City', status: 'ACTIVE' },
  { id: 5, code: 'NV0117', name: 'Ngô Đức Anh', department: 'Kỹ thuật', position: 'Kỹ thuật viên máy chiếu', cinema: 'THEFILM Aeon Tân Bình', phone: '0938 246 810', account: 'anh.ngo@thefilm.vn', role: 'STAFF', scope: 'THEFILM Aeon Tân Bình', status: 'ACTIVE' },
  { id: 6, code: 'NV0130', name: 'Lê Văn Hùng', department: 'Vận hành rạp', position: 'Quản lý rạp', cinema: 'THEFILM Indochina Đà Nẵng', phone: '0905 135 790', account: 'hung.le@thefilm.vn', role: 'CINEMA_MANAGER', scope: 'THEFILM Indochina Đà Nẵng', status: 'INACTIVE' },
];

export const MOCK_SHIFT_TEMPLATES: ShiftTemplate[] = [
  { id: 1, name: 'Sáng', timeFrom: '08:00', timeTo: '14:00' },
  { id: 2, name: 'Chiều', timeFrom: '14:00', timeTo: '20:00' },
  { id: 3, name: 'Tối', timeFrom: '18:00', timeTo: '00:30' },
];

export const MOCK_SHIFT_ASSIGNMENTS: ShiftAssignment[] = [
  ...[0, 1, 2, 3, 4].map((day, i) => ({ id: 1 + i, employeeName: 'Hoàng Thị Mai', cinema: 'THEFILM Royal City', day, shift: day < 3 ? 'Sáng' : 'Tối' })),
  ...[1, 2, 4, 5, 6].map((day, i) => ({ id: 10 + i, employeeName: 'Trần Thị Phương Thảo', cinema: 'THEFILM Royal City', day, shift: 'Chiều' })),
  ...[0, 2, 3, 5, 6].map((day, i) => ({ id: 20 + i, employeeName: 'Phạm Văn Lộc', cinema: 'THEFILM Vincom Đồng Khởi', day, shift: day >= 5 ? 'Tối' : 'Chiều' })),
  ...[0, 1, 3, 4, 5].map((day, i) => ({ id: 30 + i, employeeName: 'Ngô Đức Anh', cinema: 'THEFILM Aeon Tân Bình', day, shift: 'Sáng' })),
];

export const MOCK_ATTENDANCE: AttendanceRecord[] = [
  { id: 1, employeeName: 'Hoàng Thị Mai', cinema: 'THEFILM Royal City', date: '2026-11-13', shift: 'Sáng', checkIn: '07:56', checkOut: '14:05', lateMin: 0 },
  { id: 2, employeeName: 'Trần Thị Phương Thảo', cinema: 'THEFILM Royal City', date: '2026-11-13', shift: 'Chiều', checkIn: '14:18', checkOut: '20:02', lateMin: 18 },
  { id: 3, employeeName: 'Ngô Đức Anh', cinema: 'THEFILM Aeon Tân Bình', date: '2026-11-13', shift: 'Sáng', checkIn: '08:01', checkOut: '', lateMin: 1 },
];

export const MOCK_LEAVE_REQUESTS: LeaveRequest[] = [
  { id: 1, employeeName: 'Hoàng Thị Mai', leaveType: 'Nghỉ phép năm', fromDate: '2026-11-20', toDate: '2026-11-21', reason: 'Về quê', status: 'PENDING' },
  { id: 2, employeeName: 'Ngô Đức Anh', leaveType: 'Nghỉ ốm', fromDate: '2026-11-08', toDate: '2026-11-08', reason: 'Sốt', status: 'APPROVED' },
];

export const MOCK_PAYROLL: PayrollRun[] = [
  { id: 1, period: '10/2026', cinema: 'Toàn chuỗi', headcount: 86, gross: 1284500000, net: 1121360000, status: 'PAID' },
  { id: 2, period: '11/2026', cinema: 'Toàn chuỗi', headcount: 88, gross: 0, net: 0, status: 'DRAFT' },
];

export const MOCK_SUPPORT_MESSAGES: SupportMessage[] = [
  { id: 1, caseId: 1, from: 'CUSTOMER', author: 'Trần Bảo Ngọc', body: 'Mình thanh toán rồi mà không nhận được mã QR vé, đơn TF-261113-88421.', at: '2026-11-13T09:40:00+07:00' },
  { id: 2, caseId: 1, from: 'AI', author: 'Trợ lý AI', body: 'Đơn đã thanh toán lúc 09:13. Email gửi vé bị trả về do hộp thư đầy. Gợi ý: gửi lại vé qua SMS.', at: '2026-11-13T09:41:00+07:00' },
  { id: 3, caseId: 1, from: 'AGENT', author: 'Hoàng Thị Mai', body: 'Chào chị, em đã gửi lại vé qua SMS số 0903 123 456. Chị kiểm tra giúp em nhé.', at: '2026-11-13T09:50:00+07:00' },
];

export const MOCK_KNOWLEDGE: KnowledgeArticle[] = [
  { id: 1, title: 'Không nhận được vé điện tử sau khi thanh toán', topic: 'Đặt vé', updatedAt: '2026-11-01T10:00:00+07:00', status: 'PUBLISHED' },
  { id: 2, title: 'Chính sách hoàn vé và đổi suất', topic: 'Hoàn vé', updatedAt: '2026-10-20T10:00:00+07:00', status: 'PUBLISHED' },
  { id: 3, title: 'Cách tích và đổi điểm thành viên', topic: 'Thành viên', updatedAt: '2026-11-12T16:00:00+07:00', status: 'DRAFT' },
];

export const MOCK_AUDIT: AuditEvent[] = [
  { id: 1, at: '2026-11-13T10:22:00+07:00', actor: 'Nguyễn Minh Quân', action: 'UPDATE', entity: 'Quy tắc giá #4', detail: 'Giá 180.000 ₫ → 190.000 ₫', ip: '113.161.42.7' },
  { id: 2, at: '2026-11-13T09:05:00+07:00', actor: 'Phạm Văn Lộc', action: 'CREATE', entity: 'Suất chiếu #18', detail: 'Hồ Sơ Đêm Mưa · Gold Class 2 · 21:00', ip: '14.241.120.33' },
  { id: 3, at: '2026-11-12T22:40:00+07:00', actor: 'Trần Thị Phương Thảo', action: 'CANCEL', entity: 'Suất chiếu #11', detail: 'Lý do: sự cố máy chiếu', ip: '27.72.98.140' },
  { id: 4, at: '2026-11-12T19:31:00+07:00', actor: 'Hoàng Thị Mai', action: 'CREATE', entity: 'Bảo trì ghế E8', detail: 'Đệm ngồi rách', ip: '27.72.98.141' },
  { id: 5, at: '2026-11-12T08:00:00+07:00', actor: 'Hệ thống', action: 'LOGIN_FAILED', entity: 'Tài khoản hung.le@thefilm.vn', detail: 'Sai mật khẩu 5 lần, tạm khoá 15 phút', ip: '171.244.15.9' },
];

export const MOCK_SETTINGS: AppSettings = {
  holdMinutes: 10,
  maxTicketsPerOrder: 8,
  saleOpenDays: 14,
  refundBeforeHours: 4,
  refundFeePct: 10,
  changeBeforeHours: 2,
  paymentVnpay: true,
  paymentMomo: true,
  paymentCard: false,
  invoiceProvider: 'VNPT Invoice',
  invoiceAuto: true,
  notifyEmail: true,
  notifySms: false,
};
