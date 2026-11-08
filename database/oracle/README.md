# THEFILM Oracle database

This folder contains the first Oracle schema baseline for the cinema chain system. The migration files are numbered and Flyway-compatible. They are designed for Oracle Database 19c or newer and use Oracle identity columns.

For a single-file setup on a clean development schema, use `THEFILM_ORACLE_FULL_SCHEMA.sql`. It contains the same DDL and comments as the four migrations below, concatenated in dependency order. Do not apply the full script and the migration files to the same schema; choose one approach. For the future Spring Boot application, use the versioned migrations so Flyway can track upgrades.

## Migrations

Run these migrations in order, once per schema, using Flyway or another migration runner that records completed versions:

1. `migrations/V001__organization_catalog_schedule.sql` — chain/legal entities, Keycloak user references and cinema scopes, configurable room types/capabilities, versioned seat layouts, films and screenings.
2. `migrations/V002__pricing_booking_payments.sql` — effective-dated prices, promotions, membership points, order snapshots, seat holds, tickets, payments, callbacks and refunds.
3. `migrations/V003__tax_fnb_hr_support_audit.sql` — mock-only invoice/tax simulation, F&B catalog/inventory, employee/shift/payroll foundations, CSKH/AI records, audit, outbox and app settings.
4. `migrations/V004__vietnamese_data_dictionary.sql` — Vietnamese `COMMENT ON TABLE` and `COMMENT ON COLUMN` descriptions for all tables and fields.

After applying, inspect the data dictionary in Oracle with `SELECT table_name, comments FROM user_tab_comments` and `SELECT table_name, column_name, comments FROM user_col_comments ORDER BY table_name, column_name`.

The workspace has an Angular customer portal and the Spring Boot application in BE. The backend packages V001-V004 from this folder into its Flyway resources, so this folder remains the single migration source of truth. The consolidated script was executed successfully on local Oracle FREEPDB1 in an isolated validation schema on 2026-11-06: 83 tables, 782 columns, zero missing column comments, zero invalid objects. Configure the Oracle JDBC connection and Keycloak issuer through environment-specific settings; never commit database credentials.

## Important design rules

- Keycloak remains the identity and role provider. `app_user` stores the OIDC issuer and subject for linking business data; it stores no password, secret, refresh token or role grant. `user_cinema_scope` holds the data scope that the API must enforce alongside Keycloak roles.
- A screening gets its own `show_seat` rows from a versioned room layout. The unique key `(screening_id, room_seat_id)` prevents duplicate allocations. Booking code must lock a seat row, verify status and expiry, then update the hold/sale in one transaction. An expired hold must be released by a scheduled worker or during a locked seat recheck. Hold duration is intentionally not seeded because the requirement document marks it TBD.
- Price, promotion, room, seat, film and tax details are copied onto order/ticket/invoice records as snapshots. Updating a catalog item later must not rewrite issued history.
- Callback and points idempotency keys are persisted with uniqueness constraints. Payment timeout is a separate state and must be reconciled with the provider before it is treated as failed.
- Tax rules, VAT/PIT parameters and their effective dates are data, not hard-coded rates. The first schema enforces `simulation_only = 'Y'` and provider `MOCK`; it cannot submit an invoice to a real tax provider.
- Amounts use `NUMBER(18,2)` and currency `VND`. No legal tax rate, seat-hold duration, cancellation deadline, payment provider, or payroll policy is seeded because those values remain unconfirmed in the analysis document.
- Cross-row business rules—screening overlap/cleanup, room-layout compatibility, effective-date overlap, promotion budget, cumulative refund cap, roster overlap and inventory balance updates—must be checked transactionally by Spring Boot. Oracle row locks and unique constraints are the final protection for concurrent booking and callback processing.
- Do not physically delete cinemas, sold orders, issued tickets, invoice attempts, payroll records or audit records. Apply retention/anonymization rules after the privacy and accounting periods are agreed.

## Functional areas represented

| Area | Main tables |
| --- | --- |
| Chain, entities, cinemas, identity scope and customer profile/consent | `org_chain`, `legal_entity`, `legal_entity_profile`, `cinema`, `app_user`, `user_cinema_scope`, `customer_profile`, `customer_consent`, `user_favorite_film` |
| Rooms, room classification, seats | `room_type`, `room_capability_type`, `cinema_room`, `room_capability`, `seat_type`, `room_layout`, `room_seat`, `seat_maintenance` |
| Films and schedule | `film`, `film_genre`, `film_version`, `film_release_right`, `screening` |
| Price, voucher, member | `price_rule`, `promo_campaign`, `promo_voucher`, `promo_redemption`, `member_account`, `member_points_ledger` |
| Orders, seats, tickets and money | `sales_order`, `sales_order_line`, `show_seat`, `seat_hold`, `ticket`, `ticket_scan_event`, `ticket_change_request`, `payment_txn`, `payment_callback_event`, `refund_request`, `refund_request_line`, `refund_txn` |
| Tax simulation | `tax_category`, `tax_rule`, `tax_invoice`, `tax_invoice_line`, `tax_provider_attempt`, `pit_rule_set`, `pit_rule_bracket` |
| F&B and stock | `fnb_product`, `fnb_product_price`, `fnb_recipe_component`, `inventory_location`, `inventory_balance`, `inventory_txn`, `inventory_txn_line` |
| Staff and payroll | `employee`, `employee_assignment`, `employee_contract`, `shift_assignment`, `attendance_record`, `leave_request`, `shift_handover`, `payroll_run`, `payroll_employee`, `payroll_line` |
| Customer service and controls | `support_case`, `support_conversation`, `support_message`, `ai_interaction_log`, `knowledge_article`, `audit_event`, `integration_outbox`, `app_setting` |

## Initial rollout checklist

1. Create a dedicated Oracle schema and grant only required privileges to the application account.
2. Configure Oracle JDBC and Flyway through environment variables/secret storage.
3. Apply the numbered migrations to a disposable development schema first.
4. Confirm the organization-specific VAT/PIT demo settings and operating policies before adding reference data.
5. Implement repository/service transaction rules for seat allocation, payment callbacks, refund caps, stock posting, and cinema-level authorization before enabling transactions.


## Chạy toàn bộ một lần

Mở THEFILM_ORACLE_FULL_SCHEMA.sql bằng kết nối tới schema ứng dụng trống trong FREEPDB1, chọn Run Script (F5 trong SQL Developer). Không dùng tài khoản SYS/SYSTEM để tạo bảng ứng dụng. File chỉ chứa SQL Oracle, không chứa lệnh SET/WHENEVER của SQL*Plus. Trong DataGrip/IntelliJ, chạy toàn bộ file dưới dạng script với bộ phân tách dấu chấm phẩy; không gửi cả file như một câu lệnh duy nhất. Khi dùng SQL*Plus, cấu hình SET DEFINE OFF và WHENEVER SQLERROR ở phiên chạy bên ngoài file. Đây là script khởi tạo một lần, không phải script chạy lại trên schema đã có bảng; nếu lần trước chạy dở, hãy dùng schema mới hoặc xử lý các object đã tạo sau khi kiểm tra dữ liệu.

Ba khóa tại room_capability, fnb_recipe_component và app_setting dùng cột ảo valid_from_utc = SYS_EXTRACT_UTC(valid_from), vì Oracle không chấp nhận TIMESTAMP WITH TIME ZONE trực tiếp trong khóa chính/duy nhất. valid_from vẫn giữ đầy đủ múi giờ ban đầu.