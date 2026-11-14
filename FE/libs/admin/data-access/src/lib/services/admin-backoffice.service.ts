import { Injectable, signal } from '@angular/core';
import * as M from '../data/backoffice-mock';
import { MOCK_ADMIN_ROOMS } from '../data/admin-mock';
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
  LayoutSeat,
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
  SeatKind,
  SeatMaintenance,
  SeatType,
  ShiftAssignment,
  ShiftTemplate,
  SupportMessage,
  TaxInvoice,
  TaxRule,
} from '../models/backoffice.models';
import { MockCollection } from './mock-collection';

/**
 * Kho dữ liệu cho các trang admin chưa có API. Mỗi thuộc tính ứng với một bảng trong migration.
 * Khi BE có endpoint: thay MockCollection bằng lời gọi HttpClient, giữ nguyên tên để trang không đổi.
 */
@Injectable({ providedIn: 'root' })
export class AdminBackofficeService {
  readonly chains = new MockCollection<OrgChain>(M.MOCK_CHAINS);
  readonly legalEntities = new MockCollection<LegalEntity>(M.MOCK_LEGAL_ENTITIES);
  readonly roomTypes = new MockCollection<RoomType>(M.MOCK_ROOM_TYPES);
  readonly seatTypes = new MockCollection<SeatType>(M.MOCK_SEAT_TYPES);
  readonly seatMaintenance = new MockCollection<SeatMaintenance>(M.MOCK_SEAT_MAINTENANCE);
  readonly filmVersions = new MockCollection<FilmVersion>(M.MOCK_FILM_VERSIONS);
  readonly releaseRights = new MockCollection<ReleaseRight>(M.MOCK_RELEASE_RIGHTS);
  readonly priceRules = new MockCollection<PriceRule>(M.MOCK_PRICE_RULES);
  readonly refundRequests = new MockCollection<RefundRequest>(M.MOCK_REFUND_REQUESTS);
  readonly vouchers = new MockCollection<PromoVoucher>(M.MOCK_VOUCHERS);
  readonly memberTiers = new MockCollection<MemberTier>(M.MOCK_MEMBER_TIERS);
  readonly members = new MockCollection<Member>(M.MOCK_MEMBERS);
  readonly pointsLedger = new MockCollection<PointsTxn>(M.MOCK_POINTS_LEDGER);
  readonly fnbCategories = new MockCollection<FnbCategory>(M.MOCK_FNB_CATEGORIES);
  readonly fnbProducts = new MockCollection<FnbProduct>(M.MOCK_FNB_PRODUCTS);
  readonly inventory = new MockCollection<InventoryBalance>(M.MOCK_INVENTORY);
  readonly inventoryTxns = new MockCollection<InventoryTxn>(M.MOCK_INVENTORY_TXNS);
  readonly taxInvoices = new MockCollection<TaxInvoice>(M.MOCK_TAX_INVOICES);
  readonly taxRules = new MockCollection<TaxRule>(M.MOCK_TAX_RULES);
  readonly employees = new MockCollection<Employee>(M.MOCK_EMPLOYEES);
  readonly shiftTemplates = new MockCollection<ShiftTemplate>(M.MOCK_SHIFT_TEMPLATES);
  readonly shiftAssignments = new MockCollection<ShiftAssignment>(M.MOCK_SHIFT_ASSIGNMENTS);
  readonly attendance = new MockCollection<AttendanceRecord>(M.MOCK_ATTENDANCE);
  readonly leaveRequests = new MockCollection<LeaveRequest>(M.MOCK_LEAVE_REQUESTS);
  readonly payroll = new MockCollection<PayrollRun>(M.MOCK_PAYROLL);
  readonly supportMessages = new MockCollection<SupportMessage>(M.MOCK_SUPPORT_MESSAGES);
  readonly knowledge = new MockCollection<KnowledgeArticle>(M.MOCK_KNOWLEDGE);
  readonly audit = new MockCollection<AuditEvent>(M.MOCK_AUDIT);
  readonly settings = signal<AppSettings>({ ...M.MOCK_SETTINGS });

  private readonly layouts = new Map<number, LayoutSeat[]>();

  /** Sơ đồ ghế của phòng (room_layout đang kích hoạt + room_seat). Mock sinh theo số ghế của phòng. */
  layoutOf(roomId: number): LayoutSeat[] {
    const cached = this.layouts.get(roomId);
    if (cached) return cached;
    const room = MOCK_ADMIN_ROOMS.find((r) => r.id === roomId);
    const cols = room && room.seatCount <= 60 ? 8 : room && room.seatCount >= 250 ? 20 : 16;
    const rows = Math.max(4, Math.min(16, Math.ceil((room?.seatCount ?? 120) / cols)));
    const broken = new Set(
      this.seatMaintenance
        .items()
        .filter((m) => m.roomId === roomId && m.status === 'OPEN')
        .map((m) => m.seatLabel)
    );
    const seats: LayoutSeat[] = [];
    for (let r = 0; r < rows; r++) {
      const row = String.fromCharCode(65 + r);
      for (let c = 1; c <= cols; c++) {
        let kind: SeatKind = 'STANDARD';
        if (c === Math.floor(cols / 4) + 1 || c === cols - Math.floor(cols / 4)) kind = 'GAP';
        else if (r === rows - 1) kind = 'COUPLE';
        else if (r >= Math.floor(rows / 3) && r < rows - 2) kind = 'VIP';
        seats.push({ row, col: c, kind, broken: broken.has(`${row}${c}`) });
      }
    }
    this.layouts.set(roomId, seats);
    return seats;
  }

  saveLayout(roomId: number, seats: LayoutSeat[]): void {
    this.layouts.set(roomId, seats);
  }
}
