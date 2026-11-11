INSERT INTO age_classification VALUES (1, 'T13', 'T13 - Phim dành cho khán giả từ đủ 13 tuổi', 13, 'Nội dung phù hợp với khán giả từ đủ 13 tuổi.', 'ACTIVE');
INSERT INTO age_classification VALUES (2, 'T16', 'T16 - Phim dành cho khán giả từ đủ 16 tuổi', 16, 'Nội dung phù hợp với khán giả từ đủ 16 tuổi.', 'ACTIVE');
INSERT INTO age_classification VALUES (3, 'P', 'P - Phim phổ biến', 0, 'Phim được phổ biến đến mọi độ tuổi.', 'ACTIVE');

INSERT INTO film_genre VALUES (1, 'DRAMA', 'Tâm lý', 'ACTIVE');
INSERT INTO film_genre VALUES (2, 'ADVENTURE', 'Phiêu lưu', 'ACTIVE');
INSERT INTO film_genre VALUES (3, 'MYSTERY', 'Bí ẩn', 'ACTIVE');
INSERT INTO film_genre VALUES (4, 'FAMILY', 'Gia đình', 'ACTIVE');

INSERT INTO cinema VALUES (1, 'HCM-01', 'THEFILM Nguyễn Trãi', '123 Nguyễn Trãi', 'TP. Hồ Chí Minh', 'Quận 1', 'Phường Bến Thành', '02800000001', 'nguyentrai@thefilm.vn', 'Asia/Ho_Chi_Minh', 'ACTIVE');
INSERT INTO cinema VALUES (2, 'HN-01', 'THEFILM Hồ Đền', '45 Hồ Đền', 'Hà Nội', 'Đống Đa', 'Phường Trung Liệt', '02400000002', 'hoden@thefilm.vn', 'Asia/Ho_Chi_Minh', 'ACTIVE');

INSERT INTO room_type VALUES (1, 'STANDARD', 'Standard', 'ACTIVE');
INSERT INTO room_type VALUES (2, 'VIP', 'VIP', 'ACTIVE');
INSERT INTO room_type VALUES (3, 'GOLD_CLASS', 'Gold Class', 'ACTIVE');
INSERT INTO cinema_room VALUES (1, 1, 1, 'P01', 'Phòng 01', 'ACTIVE');
INSERT INTO cinema_room VALUES (2, 1, 2, 'VIP01', 'Phòng VIP 01', 'ACTIVE');
INSERT INTO cinema_room VALUES (3, 2, 1, 'P01', 'Phòng 01', 'ACTIVE');
INSERT INTO cinema_room VALUES (4, 1, 3, 'GOLD03', 'Phòng 03 (Gold Class)', 'ACTIVE');

INSERT INTO film VALUES (1, 'THEFILM-001', 'Tầng Mây Cuối Cùng', 'The Last Cloud', 'Một chuyến phiêu lưu đưa hai người xa lạ đến gần nhau hơn.', 126, 1, 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=900&q=80', NULL, 'APPROVED');
INSERT INTO film VALUES (2, 'THEFILM-002', 'Mùa Hè Bất Tận', 'Endless Summer', 'Câu chuyện về tình bạn, gia đình và những lựa chọn trưởng thành.', 108, 2, 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=900&q=80', NULL, 'APPROVED');
INSERT INTO film VALUES (3, 'THEFILM-003', 'Hồ Sơ Đêm Mưa', 'Rainy Night Files', 'Một bí ẩn bắt đầu sau chuyến tàu cuối cùng trong đêm mưa.', 115, 2, 'https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=900&q=80', NULL, 'APPROVED');
INSERT INTO film VALUES (4, 'THEFILM-004', 'Khu Rừng Kỳ Diệu', 'The Wonder Woods', 'Một gia đình khám phá khu rừng ẩn giấu nhiều điều kỳ diệu.', 98, 3, 'https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=900&q=80', NULL, 'APPROVED');

INSERT INTO film_genre_map VALUES (1, 1);
INSERT INTO film_genre_map VALUES (1, 2);
INSERT INTO film_genre_map VALUES (2, 1);
INSERT INTO film_genre_map VALUES (2, 4);
INSERT INTO film_genre_map VALUES (3, 3);
INSERT INTO film_genre_map VALUES (4, 2);
INSERT INTO film_genre_map VALUES (4, 4);

INSERT INTO film_version VALUES (1, 1, 'VIETSUB-2D', '2D', 126, 'ACTIVE');
INSERT INTO film_version VALUES (2, 2, 'VIETSUB-2D', '2D', 108, 'ACTIVE');
INSERT INTO film_version VALUES (3, 3, 'VIETSUB-2D', '2D', 115, 'ACTIVE');
INSERT INTO film_version VALUES (4, 4, 'VIETSUB-2D', '2D', 98, 'ACTIVE');

INSERT INTO screening VALUES (1, 1, 1, 1, 1, 1, 'STANDARD', 'Standard', '2D', 'REGULAR', CAST(CURRENT_TIMESTAMP + INTERVAL '3' HOUR AS TIMESTAMP WITH TIME ZONE), CAST(CURRENT_TIMESTAMP + INTERVAL '5' HOUR AS TIMESTAMP WITH TIME ZONE), 'ON_SALE');
INSERT INTO screening VALUES (2, 1, 2, 1, 2, 2, 'VIP', 'VIP', '2D', 'REGULAR', CAST(CURRENT_TIMESTAMP + INTERVAL '6' HOUR AS TIMESTAMP WITH TIME ZONE), CAST(CURRENT_TIMESTAMP + INTERVAL '8' HOUR AS TIMESTAMP WITH TIME ZONE), 'ON_SALE');
INSERT INTO screening VALUES (3, 2, 3, 1, 3, 3, 'STANDARD', 'Standard', '2D', 'REGULAR', CAST(CURRENT_TIMESTAMP + INTERVAL '30' HOUR AS TIMESTAMP WITH TIME ZONE), CAST(CURRENT_TIMESTAMP + INTERVAL '32' HOUR AS TIMESTAMP WITH TIME ZONE), 'SCHEDULED');
-- Suất demo st-05 có ID riêng, không ánh xạ sang suất của phòng hoặc phim khác.
INSERT INTO screening VALUES (5, 1, 4, 1, 1, 1, 'GOLD_CLASS', 'Gold Class', '2D', 'REGULAR', CAST(CURRENT_TIMESTAMP + INTERVAL '3' HOUR AS TIMESTAMP WITH TIME ZONE), CAST(CURRENT_TIMESTAMP + INTERVAL '5' HOUR AS TIMESTAMP WITH TIME ZONE), 'ON_SALE');

INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status, seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
VALUES (1, 1, 1, 'AVAILABLE', 'A1', 'A', 'A1', 'STANDARD');
INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status, seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
VALUES (2, 1, 2, 'AVAILABLE', 'A2', 'A', 'A2', 'STANDARD');
INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status, seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
VALUES (3, 1, 3, 'SOLD', 'A3', 'A', 'A3', 'STANDARD');
INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status, seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
VALUES (4, 2, 4, 'AVAILABLE', 'A4', 'A', 'A4', 'STANDARD');
INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status, hold_expires_at,
                      seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
VALUES (5, 2, 5, 'HELD', CURRENT_TIMESTAMP - INTERVAL '1' MINUTE, 'A5', 'A', 'A5', 'STANDARD');

INSERT INTO seat_type VALUES (1, 'STANDARD', 'Ghế tiêu chuẩn', 'ACTIVE');
INSERT INTO seat_type VALUES (2, 'VIP', 'Ghế VIP', 'ACTIVE');
INSERT INTO seat_type VALUES (3, 'COUPLE', 'Ghế đôi', 'ACTIVE');

INSERT INTO room_seat (room_seat_id, room_id, seat_type_id, seat_code, row_label, seat_label, status)
SELECT X,
       1,
       CASE WHEN X > 90 THEN 3 WHEN X > 80 THEN 2 ELSE 1 END,
       SUBSTRING('ABCDEFGHIJ', CAST(FLOOR((X - 1) / 10) + 1 AS INTEGER), 1) || CAST(MOD(X - 1, 10) + 1 AS VARCHAR),
       SUBSTRING('ABCDEFGHIJ', CAST(FLOOR((X - 1) / 10) + 1 AS INTEGER), 1),
       SUBSTRING('ABCDEFGHIJ', CAST(FLOOR((X - 1) / 10) + 1 AS INTEGER), 1) || CAST(MOD(X - 1, 10) + 1 AS VARCHAR),
       'ACTIVE'
FROM SYSTEM_RANGE(1, 100);

INSERT INTO room_seat (room_seat_id, room_id, seat_type_id, seat_code, row_label, seat_label, status)
SELECT 100 + X, 4, CASE WHEN X > 40 THEN 3 WHEN X > 32 THEN 2 ELSE 1 END,
       SUBSTRING('ABCDEF', CAST(FLOOR((X - 1) / 8) + 1 AS INTEGER), 1) || CAST(MOD(X - 1, 8) + 1 AS VARCHAR),
       SUBSTRING('ABCDEF', CAST(FLOOR((X - 1) / 8) + 1 AS INTEGER), 1),
       SUBSTRING('ABCDEF', CAST(FLOOR((X - 1) / 8) + 1 AS INTEGER), 1) || CAST(MOD(X - 1, 8) + 1 AS VARCHAR), 'ACTIVE'
FROM SYSTEM_RANGE(1, 48);
INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status, seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
SELECT 200 + X, 5, 100 + X, 'AVAILABLE', rs.seat_code, rs.row_label, rs.seat_label,
       CASE WHEN X > 40 THEN 'COUPLE' WHEN X > 32 THEN 'VIP' ELSE 'STANDARD' END
FROM SYSTEM_RANGE(1, 48) JOIN room_seat rs ON rs.room_seat_id = 100 + X;

-- Complete the first screening's seat inventory for integration scenarios and local booking flows.
INSERT INTO show_seat (show_seat_id, screening_id, room_seat_id, seat_status,
                       seat_code_snapshot, row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
SELECT X,
       1,
       X,
       'AVAILABLE',
       SUBSTRING('ABCDEFGHIJ', CAST(FLOOR((X - 1) / 10) + 1 AS INTEGER), 1) || CAST(MOD(X - 1, 10) + 1 AS VARCHAR),
       SUBSTRING('ABCDEFGHIJ', CAST(FLOOR((X - 1) / 10) + 1 AS INTEGER), 1),
       SUBSTRING('ABCDEFGHIJ', CAST(FLOOR((X - 1) / 10) + 1 AS INTEGER), 1) || CAST(MOD(X - 1, 10) + 1 AS VARCHAR),
       CASE WHEN X > 90 THEN 'COUPLE' WHEN X > 80 THEN 'VIP' ELSE 'STANDARD' END
FROM SYSTEM_RANGE(6, 100);

INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code,
                        valid_from, base_price, priority_no, approval_status)
VALUES (1, 'HCM01-STANDARD-BASE', 1, 1, 1, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 75000, 100, 'APPROVED');
INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code,
                        valid_from, base_price, priority_no, approval_status)
VALUES (2, 'HCM01-VIP-BASE', 1, 1, 2, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 110000, 100, 'APPROVED');
INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code,
                        valid_from, base_price, priority_no, approval_status)
VALUES (3, 'HCM01-COUPLE-BASE', 1, 1, 3, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 180000, 100, 'APPROVED');
INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code,
                        valid_from, base_price, priority_no, approval_status)
VALUES (4, 'HCM01-VIP-ROOM', 1, 2, 2, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 130000, 100, 'APPROVED');
INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code, valid_from, base_price, priority_no, approval_status)
VALUES (5, 'HCM01-GOLD-STANDARD', 1, 3, 1, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 180000, 100, 'APPROVED');
INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code, valid_from, base_price, priority_no, approval_status)
VALUES (6, 'HCM01-GOLD-VIP', 1, 3, 2, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 220000, 100, 'APPROVED');
INSERT INTO price_rule (price_rule_id, rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code, valid_from, base_price, priority_no, approval_status)
VALUES (7, 'HCM01-GOLD-COUPLE', 1, 3, 3, 'REGULAR', CURRENT_TIMESTAMP - INTERVAL '30' DAY, 450000, 100, 'APPROVED');

INSERT INTO promo_campaign (promo_campaign_id, campaign_code, campaign_name, discount_type, discount_value,
                            max_discount, valid_from, valid_to, stackable, status)
VALUES (1, 'WELCOME10', 'Ưu đãi khách hàng mới', 'PERCENT', 10, 50000,
        CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '90' DAY, 'N', 'ACTIVE');
INSERT INTO promo_campaign (promo_campaign_id, campaign_code, campaign_name, discount_type, discount_value,
                            max_discount, valid_from, valid_to, stackable, status)
VALUES (2, 'MOVIE50000', 'Giảm 50.000 đồng', 'FIXED', 50000, 50000,
        CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '90' DAY, 'N', 'ACTIVE');
INSERT INTO promo_voucher (voucher_id, promo_campaign_id, voucher_code, valid_from, valid_to, status)
VALUES (1, 1, 'THEFILM-WELCOME10', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '90' DAY, 'ACTIVE');
INSERT INTO promo_voucher (voucher_id, promo_campaign_id, voucher_code, valid_from, valid_to, status)
VALUES (2, 2, 'THEFILM-50000', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '90' DAY, 'ACTIVE');

-- Local identity examples; BCrypt hash nằm ở bảng local_credential (mật khẩu: 123456).
INSERT INTO app_user (app_user_id, oidc_issuer, oidc_subject, display_name, email, phone, status)
VALUES (1, 'https://identity.thefilm.local/realms/thefilm', 'customer-demo-001', 'Nguyễn Minh Anh', 'minhanh@example.test', '09000000001', 'ACTIVE');
INSERT INTO app_user (app_user_id, oidc_issuer, oidc_subject, display_name, email, phone, status)
VALUES (2, 'https://identity.thefilm.local/realms/thefilm', 'employee-demo-001', 'Trần Quốc Bảo', 'quocbao@example.test', '09000000002', 'ACTIVE');
INSERT INTO local_credential (app_user_id, password_hash) VALUES (1, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');
INSERT INTO local_credential (app_user_id, password_hash) VALUES (2, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');

INSERT INTO customer_profile (app_user_id, preferred_cinema_id, preferred_locale, date_of_birth, profile_status)
VALUES (1, 1, 'vi-VN', DATE '1998-05-20', 'ACTIVE');
INSERT INTO customer_consent (customer_consent_id, app_user_id, consent_type, policy_version, consent_status, captured_channel)
VALUES (1, 1, 'TERMS', '2026-01', 'GRANTED', 'WEB');
INSERT INTO customer_consent (customer_consent_id, app_user_id, consent_type, policy_version, consent_status, captured_channel)
VALUES (2, 1, 'PRIVACY', '2026-01', 'GRANTED', 'WEB');
INSERT INTO user_cinema_scope (user_cinema_scope_id, app_user_id, cinema_id, created_by)
VALUES (1, 2, 1, 2);

INSERT INTO employee (employee_id, chain_id, app_user_id, employee_code, full_name, phone, email, status, hired_on)
VALUES (1, 1, 2, 'TF-HCM-0001', 'Trần Quốc Bảo', '09000000002', 'quocbao@example.test', 'ACTIVE', DATE '2025-06-01');
INSERT INTO employee_assignment (employee_assignment_id, employee_id, cinema_id, job_position_id, valid_from, assignment_status, changed_by)
VALUES (1, 1, 1, 1, DATE '2025-06-01', 'ACTIVE', 2);
INSERT INTO audit_event (audit_event_id, actor_user_id, cinema_id, action_code, object_type, object_id, trace_id, source_ip)
VALUES (1, 2, 1, 'EMPLOYEE_ASSIGNMENT_CREATED', 'EMPLOYEE_ASSIGNMENT', '1', 'local-seed-001', '127.0.0.1');

-- Allocate generated identifiers above the deterministic local seed data.
ALTER TABLE app_user ALTER COLUMN app_user_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE show_seat ALTER COLUMN show_seat_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE sales_order ALTER COLUMN sales_order_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE sales_order_line ALTER COLUMN order_line_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE seat_hold ALTER COLUMN seat_hold_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE payment_txn ALTER COLUMN payment_txn_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE ticket ALTER COLUMN ticket_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE price_rule ALTER COLUMN price_rule_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE customer_consent ALTER COLUMN customer_consent_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE user_cinema_scope ALTER COLUMN user_cinema_scope_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE employee ALTER COLUMN employee_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE employee_assignment ALTER COLUMN employee_assignment_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
ALTER TABLE audit_event ALTER COLUMN audit_event_id BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1000);
