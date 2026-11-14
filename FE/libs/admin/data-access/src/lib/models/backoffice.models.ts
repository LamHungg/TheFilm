// Model cho các nghiệp vụ admin chưa có API (bám theo bảng trong migration V001–V003).
// Tên trường dùng camelCase của bảng tương ứng, để BE map thẳng khi viết API.

export type ActiveStatus = 'ACTIVE' | 'INACTIVE';
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/* ===== Tổ chức: org_chain, legal_entity ===== */
export interface OrgChain {
  id: number;
  code: string;
  name: string;
  hotline: string;
  website: string;
  status: ActiveStatus;
}

export interface LegalEntity {
  id: number;
  code: string;
  name: string;
  chainName: string;
  taxCode: string;
  address: string;
  representative: string;
  status: ActiveStatus;
}

/* ===== Cấu hình phòng: room_type, seat_type, room_layout, room_seat, seat_maintenance ===== */
export interface RoomType {
  id: number;
  code: string;
  name: string;
  description: string;
  surcharge: number;
  status: ActiveStatus;
}

export interface SeatType {
  id: number;
  code: string;
  name: string;
  seatsPerUnit: number;
  surcharge: number;
  status: ActiveStatus;
}

export type SeatKind = 'STANDARD' | 'VIP' | 'COUPLE' | 'GAP';

export interface LayoutSeat {
  row: string;
  col: number;
  kind: SeatKind;
  broken: boolean;
}

export interface SeatMaintenance {
  id: number;
  roomId: number;
  cinemaName: string;
  roomName: string;
  seatLabel: string;
  reason: string;
  reportedAt: string;
  status: 'OPEN' | 'RESOLVED';
}

/* ===== Phim: film_version, film_release_right ===== */
export interface FilmVersion {
  id: number;
  filmTitle: string;
  format: '2D' | '3D' | 'IMAX' | '4DX' | 'SCREENX';
  audio: 'SUBTITLE' | 'DUBBED' | 'VOICEOVER';
  language: string;
  durationMin: number;
  status: ActiveStatus;
}

export interface ReleaseRight {
  id: number;
  filmTitle: string;
  distributor: string;
  cinemaScope: string;
  startDate: string;
  endDate: string;
  revenueSharePct: number;
  status: 'ACTIVE' | 'PENDING' | 'EXPIRED';
}

/* ===== Giá vé: price_rule ===== */
export interface PriceRule {
  id: number;
  name: string;
  cinemaScope: string;
  roomType: string;
  seatType: string;
  dayType: 'WEEKDAY' | 'WEEKEND' | 'HOLIDAY';
  timeFrom: string;
  timeTo: string;
  price: number;
  priority: number;
  status: ActiveStatus;
}

/* ===== Hoàn & đổi vé: refund_request, ticket_change_request ===== */
export interface RefundRequest {
  id: number;
  code: string;
  orderCode: string;
  customerName: string;
  kind: 'REFUND' | 'CHANGE';
  reason: string;
  amount: number;
  createdAt: string;
  status: ApprovalStatus;
}

/* ===== Khuyến mãi: promo_voucher ===== */
export interface PromoVoucher {
  id: number;
  code: string;
  campaign: string;
  issuedTo: string;
  expiresAt: string;
  status: 'UNUSED' | 'USED' | 'EXPIRED';
}

/* ===== Thành viên: member_tier, member_account, member_points_ledger ===== */
export interface MemberTier {
  id: number;
  code: string;
  name: string;
  minSpend: number;
  pointRatePct: number;
  perks: string;
  status: ActiveStatus;
}

export interface Member {
  id: number;
  code: string;
  name: string;
  phone: string;
  email: string;
  tier: string;
  points: number;
  totalSpend: number;
  joinedAt: string;
  status: ActiveStatus;
}

export interface PointsTxn {
  id: number;
  memberName: string;
  delta: number;
  reason: string;
  at: string;
}

/* ===== Bắp nước: fnb_category, fnb_product, inventory_balance, inventory_txn ===== */
export interface FnbCategory {
  id: number;
  name: string;
  sortOrder: number;
  status: ActiveStatus;
}

export interface FnbProduct {
  id: number;
  sku: string;
  name: string;
  category: string;
  price: number;
  cinemaScope: string;
  status: ActiveStatus;
}

export interface InventoryBalance {
  id: number;
  cinemaName: string;
  item: string;
  unit: string;
  onHand: number;
  minLevel: number;
}

export interface InventoryTxn {
  id: number;
  code: string;
  cinemaName: string;
  kind: 'IMPORT' | 'EXPORT' | 'ADJUST';
  item: string;
  qty: number;
  at: string;
  by: string;
}

/* ===== Hoá đơn: tax_invoice, tax_rule ===== */
export interface TaxInvoice {
  id: number;
  number: string;
  orderCode: string;
  legalEntity: string;
  buyer: string;
  amount: number;
  vat: number;
  issuedAt: string;
  status: 'ISSUED' | 'PENDING' | 'FAILED';
  error?: string;
}

export interface TaxRule {
  id: number;
  category: string;
  ratePct: number;
  effectiveFrom: string;
  note: string;
}

/* ===== Nhân sự: employee, employee_assignment, user_cinema_scope, shift_*, attendance_*, leave_*, payroll_* ===== */
export interface Employee {
  id: number;
  code: string;
  name: string;
  department: string;
  position: string;
  cinema: string;
  phone: string;
  account: string;
  role: 'CHAIN_ADMIN' | 'CINEMA_MANAGER' | 'STAFF';
  scope: string;
  status: ActiveStatus;
}

export interface ShiftTemplate {
  id: number;
  name: string;
  timeFrom: string;
  timeTo: string;
}

export interface ShiftAssignment {
  id: number;
  employeeName: string;
  cinema: string;
  /** 0 = Thứ Hai … 6 = Chủ nhật của tuần đang xem. */
  day: number;
  shift: string;
}

export interface AttendanceRecord {
  id: number;
  employeeName: string;
  cinema: string;
  date: string;
  shift: string;
  checkIn: string;
  checkOut: string;
  lateMin: number;
}

export interface LeaveRequest {
  id: number;
  employeeName: string;
  leaveType: string;
  fromDate: string;
  toDate: string;
  reason: string;
  status: ApprovalStatus;
}

export interface PayrollRun {
  id: number;
  period: string;
  cinema: string;
  headcount: number;
  gross: number;
  net: number;
  status: 'DRAFT' | 'LOCKED' | 'PAID';
}

/* ===== CSKH: support_message, knowledge_article ===== */
export interface SupportMessage {
  id: number;
  caseId: number;
  from: 'CUSTOMER' | 'AGENT' | 'AI';
  author: string;
  body: string;
  at: string;
}

export interface KnowledgeArticle {
  id: number;
  title: string;
  topic: string;
  updatedAt: string;
  status: 'PUBLISHED' | 'DRAFT';
}

/* ===== Hệ thống: audit_event, app_setting ===== */
export interface AuditEvent {
  id: number;
  at: string;
  actor: string;
  action: string;
  entity: string;
  detail: string;
  ip: string;
}

export interface AppSettings {
  holdMinutes: number;
  maxTicketsPerOrder: number;
  saleOpenDays: number;
  refundBeforeHours: number;
  refundFeePct: number;
  changeBeforeHours: number;
  paymentVnpay: boolean;
  paymentMomo: boolean;
  paymentCard: boolean;
  invoiceProvider: string;
  invoiceAuto: boolean;
  notifyEmail: boolean;
  notifySms: boolean;
}
