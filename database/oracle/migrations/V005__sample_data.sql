-- THEFILM Oracle schema - V005
-- Du lieu mau cho moi truong phat trien va demo. Ten cong ty, ma so thue, dia chi, so dien thoai,
-- email va nguoi dung trong file nay deu la du lieu gia lap (email dung ten mien .example).
-- Noi dung: chuoi rap, phap nhan, rap, phong, so do ghe, phim, quyen phat hanh, suat chieu qua khu va
-- 7 ngay toi, bang gia, khuyen mai, quay F&B, nhan vien, tai khoan, hoi vien, don hang, ve, thanh toan,
-- hoan tien, hoa don mo phong, ton kho, cham cong, bang luong mo phong, ho tro khach hang.
-- Cac moc thoi gian tinh theo thoi diem migration chay, nen chi chay mot lan khi khoi tao du lieu mau.
-- Moi truong that: bo file nay khoi thu muc migration (xem README).

-- 1. Chuoi, phap nhan, rap
INSERT INTO org_chain (chain_code, chain_name)
VALUES ('THEFILM', 'Hệ thống rạp chiếu phim THEFILM');

INSERT INTO legal_entity (chain_id, entity_code, legal_name, tax_identifier)
SELECT chain_id, 'LE-HCM', 'Công ty Cổ phần Giải trí THEFILM Miền Nam', '0312000001'
FROM org_chain WHERE chain_code = 'THEFILM';

INSERT INTO legal_entity (chain_id, entity_code, legal_name, tax_identifier)
SELECT chain_id, 'LE-HN', 'Công ty Cổ phần Giải trí THEFILM Miền Bắc', '0108000002'
FROM org_chain WHERE chain_code = 'THEFILM';

INSERT INTO cinema (chain_id, legal_entity_id, cinema_code, cinema_name, address_line, province_name,
                    district_name, ward_name, phone, email, time_zone)
SELECT c.chain_id, e.legal_entity_id, 'HCM-Q1', 'THEFILM Nguyễn Huệ',
       'Tầng 5, Trung tâm thương mại Nguyễn Huệ, 88 Nguyễn Huệ', 'TP. Hồ Chí Minh', 'Quận 1',
       'Phường Bến Nghé', '028 3822 0001', 'nguyenhue@thefilm.example', 'Asia/Ho_Chi_Minh'
FROM org_chain c JOIN legal_entity e ON e.chain_id = c.chain_id AND e.entity_code = 'LE-HCM'
WHERE c.chain_code = 'THEFILM';

INSERT INTO cinema (chain_id, legal_entity_id, cinema_code, cinema_name, address_line, province_name,
                    district_name, ward_name, phone, email, time_zone)
SELECT c.chain_id, e.legal_entity_id, 'HCM-TD', 'THEFILM Thủ Đức',
       'Tầng 3, Trung tâm thương mại Thủ Đức, 216 Võ Văn Ngân', 'TP. Hồ Chí Minh', 'Thành phố Thủ Đức',
       'Phường Bình Thọ', '028 3722 0002', 'thuduc@thefilm.example', 'Asia/Ho_Chi_Minh'
FROM org_chain c JOIN legal_entity e ON e.chain_id = c.chain_id AND e.entity_code = 'LE-HCM'
WHERE c.chain_code = 'THEFILM';

INSERT INTO cinema (chain_id, legal_entity_id, cinema_code, cinema_name, address_line, province_name,
                    district_name, ward_name, phone, email, time_zone)
SELECT c.chain_id, e.legal_entity_id, 'HN-CG', 'THEFILM Cầu Giấy',
       'Tầng 4, Trung tâm thương mại Cầu Giấy, 99 Trần Thái Tông', 'Hà Nội', 'Quận Cầu Giấy',
       'Phường Dịch Vọng Hậu', '024 3556 0003', 'caugiay@thefilm.example', 'Asia/Ho_Chi_Minh'
FROM org_chain c JOIN legal_entity e ON e.chain_id = c.chain_id AND e.entity_code = 'LE-HN'
WHERE c.chain_code = 'THEFILM';

INSERT INTO cinema (chain_id, legal_entity_id, cinema_code, cinema_name, address_line, province_name,
                    district_name, ward_name, phone, email, time_zone)
SELECT c.chain_id, e.legal_entity_id, 'DN-HC', 'THEFILM Hải Châu',
       'Tầng 3, Trung tâm thương mại Hải Châu, 255 Hùng Vương', 'Đà Nẵng', 'Quận Hải Châu',
       'Phường Vĩnh Trung', '0236 3822 0004', 'haichau@thefilm.example', 'Asia/Ho_Chi_Minh'
FROM org_chain c JOIN legal_entity e ON e.chain_id = c.chain_id AND e.entity_code = 'LE-HCM'
WHERE c.chain_code = 'THEFILM';

-- 2. Danh muc loai phong, tien ich phong, loai ghe
INSERT INTO room_type (type_code, type_name, description)
VALUES ('STANDARD', 'Phòng tiêu chuẩn', 'Phòng chiếu 2D/3D tiêu chuẩn với ghế thường, ghế VIP và ghế đôi.');
INSERT INTO room_type (type_code, type_name, description)
VALUES ('PREMIUM', 'Phòng cao cấp', 'Phòng quy mô nhỏ, toàn bộ ghế VIP ngả lưng, phục vụ đồ ăn tại chỗ.');
INSERT INTO room_type (type_code, type_name, description)
VALUES ('LARGE_FORMAT', 'Phòng đại định dạng', 'Màn hình lớn, âm thanh vòm nhiều kênh, phù hợp phim bom tấn.');

INSERT INTO room_capability_type (capability_code, capability_name, capability_group)
VALUES ('SOUND_SURROUND', 'Âm thanh vòm nhiều kênh', 'AUDIO');
INSERT INTO room_capability_type (capability_code, capability_name, capability_group)
VALUES ('PROJECTION_LASER', 'Máy chiếu laser', 'VIDEO');
INSERT INTO room_capability_type (capability_code, capability_name, capability_group)
VALUES ('RECLINER_SEAT', 'Ghế ngả lưng', 'COMFORT');
INSERT INTO room_capability_type (capability_code, capability_name, capability_group)
VALUES ('ACCESSIBLE_ENTRY', 'Lối vào cho xe lăn', 'ACCESSIBILITY');

INSERT INTO seat_type (type_code, type_name, description)
VALUES ('STANDARD', 'Ghế thường', 'Ghế tiêu chuẩn các hàng đầu và giữa phòng.');
INSERT INTO seat_type (type_code, type_name, description)
VALUES ('VIP', 'Ghế VIP', 'Ghế vị trí đẹp, đệm dày, khoảng cách hàng rộng.');
INSERT INTO seat_type (type_code, type_name, description)
VALUES ('COUPLE', 'Ghế đôi', 'Ghế đôi liền nhau dành cho hai người, tính giá theo từng chỗ.');

-- 3. Phan loai do tuoi va the loai phim
INSERT INTO age_classification (classification_code, display_name, minimum_age, description)
VALUES ('P', 'P - Mọi lứa tuổi', 0, 'Phim được phép phổ biến đến mọi đối tượng.');
INSERT INTO age_classification (classification_code, display_name, minimum_age, description)
VALUES ('K', 'K - Dưới 13 tuổi có người giám hộ', 0, 'Phim phổ biến đến khán giả dưới 13 tuổi khi xem cùng cha mẹ hoặc người giám hộ.');
INSERT INTO age_classification (classification_code, display_name, minimum_age, description)
VALUES ('T13', 'T13 - Từ đủ 13 tuổi', 13, 'Phim phổ biến đến khán giả từ đủ 13 tuổi trở lên.');
INSERT INTO age_classification (classification_code, display_name, minimum_age, description)
VALUES ('T16', 'T16 - Từ đủ 16 tuổi', 16, 'Phim phổ biến đến khán giả từ đủ 16 tuổi trở lên.');
INSERT INTO age_classification (classification_code, display_name, minimum_age, description)
VALUES ('T18', 'T18 - Từ đủ 18 tuổi', 18, 'Phim phổ biến đến khán giả từ đủ 18 tuổi trở lên.');

INSERT INTO film_genre (genre_code, genre_name) VALUES ('ACTION', 'Hành động');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('ADVENTURE', 'Phiêu lưu');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('ANIMATION', 'Hoạt hình');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('COMEDY', 'Hài');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('CRIME', 'Hình sự');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('DRAMA', 'Tâm lý');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('FAMILY', 'Gia đình');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('HORROR', 'Kinh dị');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('ROMANCE', 'Lãng mạn');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('SCIFI', 'Khoa học viễn tưởng');
INSERT INTO film_genre (genre_code, genre_name) VALUES ('THRILLER', 'Giật gân');

-- 4. Phong chieu va tien ich phong
INSERT INTO cinema_room (cinema_id, room_type_id, room_code, room_name)
SELECT c.cinema_id, t.room_type_id, r.room_code, r.room_name
FROM (
    SELECT 'HCM-Q1' cinema_code, 'P01' room_code, 'Phòng 1' room_name, 'STANDARD' type_code FROM dual UNION ALL
    SELECT 'HCM-Q1', 'P02', 'Phòng 2', 'STANDARD' FROM dual UNION ALL
    SELECT 'HCM-Q1', 'P03', 'Phòng 3 Premium', 'PREMIUM' FROM dual UNION ALL
    SELECT 'HCM-Q1', 'P04', 'Phòng 4 Đại định dạng', 'LARGE_FORMAT' FROM dual UNION ALL
    SELECT 'HCM-TD', 'P01', 'Phòng 1', 'STANDARD' FROM dual UNION ALL
    SELECT 'HCM-TD', 'P02', 'Phòng 2', 'STANDARD' FROM dual UNION ALL
    SELECT 'HCM-TD', 'P03', 'Phòng 3 Premium', 'PREMIUM' FROM dual UNION ALL
    SELECT 'HN-CG', 'P01', 'Phòng 1', 'STANDARD' FROM dual UNION ALL
    SELECT 'HN-CG', 'P02', 'Phòng 2', 'STANDARD' FROM dual UNION ALL
    SELECT 'HN-CG', 'P03', 'Phòng 3 Đại định dạng', 'LARGE_FORMAT' FROM dual UNION ALL
    SELECT 'DN-HC', 'P01', 'Phòng 1', 'STANDARD' FROM dual UNION ALL
    SELECT 'DN-HC', 'P02', 'Phòng 2', 'STANDARD' FROM dual UNION ALL
    SELECT 'DN-HC', 'P03', 'Phòng 3 Premium', 'PREMIUM' FROM dual
) r
JOIN cinema c ON c.cinema_code = r.cinema_code
JOIN room_type t ON t.type_code = r.type_code;

INSERT INTO room_capability (room_id, capability_type_id, capability_value)
SELECT cr.room_id, ct.capability_type_id, 'Có'
FROM cinema_room cr
JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
JOIN room_type rt ON rt.room_type_id = cr.room_type_id
JOIN room_capability_type ct ON ct.capability_code = 'ACCESSIBLE_ENTRY'
WHERE rt.type_code IN ('STANDARD', 'PREMIUM', 'LARGE_FORMAT');

INSERT INTO room_capability (room_id, capability_type_id, capability_value)
SELECT cr.room_id, ct.capability_type_id, 'Hệ thống 7.1 kênh'
FROM cinema_room cr
JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
JOIN room_type rt ON rt.room_type_id = cr.room_type_id
JOIN room_capability_type ct ON ct.capability_code = 'SOUND_SURROUND'
WHERE rt.type_code IN ('PREMIUM', 'LARGE_FORMAT');

INSERT INTO room_capability (room_id, capability_type_id, capability_value)
SELECT cr.room_id, ct.capability_type_id, 'Có'
FROM cinema_room cr
JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
JOIN room_type rt ON rt.room_type_id = cr.room_type_id
JOIN room_capability_type ct ON ct.capability_code = 'PROJECTION_LASER'
WHERE rt.type_code = 'LARGE_FORMAT';

INSERT INTO room_capability (room_id, capability_type_id, capability_value)
SELECT cr.room_id, ct.capability_type_id, 'Có'
FROM cinema_room cr
JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
JOIN room_type rt ON rt.room_type_id = cr.room_type_id
JOIN room_capability_type ct ON ct.capability_code = 'RECLINER_SEAT'
WHERE rt.type_code = 'PREMIUM';

-- 5. So do ghe (phien ban 1, dang ap dung) va ghe tung phong
DECLARE
    v_layout NUMBER;

    PROCEDURE add_row(p_layout NUMBER, p_row VARCHAR2, p_count NUMBER, p_type VARCHAR2) IS
        v_type NUMBER;
    BEGIN
        SELECT seat_type_id INTO v_type FROM seat_type WHERE type_code = p_type;
        FOR c IN 1 .. p_count LOOP
            INSERT INTO room_seat (layout_id, seat_type_id, seat_code, row_label, column_no, seat_label)
            VALUES (p_layout, v_type, p_row || c, p_row, c, p_row || c);
        END LOOP;
    END;
BEGIN
    FOR r IN (
        SELECT cr.room_id, rt.type_code
        FROM cinema_room cr
        JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
        JOIN room_type rt ON rt.room_type_id = cr.room_type_id
        WHERE NOT EXISTS (SELECT 1 FROM room_layout l WHERE l.room_id = cr.room_id)
        ORDER BY cr.room_id
    ) LOOP
        INSERT INTO room_layout (room_id, version_no, layout_name, status, valid_from)
        VALUES (r.room_id, 1, 'Sơ đồ ghế chuẩn', 'ACTIVE', SYSTIMESTAMP - INTERVAL '60' DAY)
        RETURNING layout_id INTO v_layout;

        IF r.type_code = 'STANDARD' THEN
            FOR i IN 0 .. 3 LOOP add_row(v_layout, CHR(65 + i), 12, 'STANDARD'); END LOOP;
            FOR i IN 4 .. 7 LOOP add_row(v_layout, CHR(65 + i), 12, 'VIP'); END LOOP;
            add_row(v_layout, 'J', 6, 'COUPLE');
        ELSIF r.type_code = 'PREMIUM' THEN
            FOR i IN 0 .. 5 LOOP add_row(v_layout, CHR(65 + i), 10, 'VIP'); END LOOP;
            add_row(v_layout, 'G', 5, 'COUPLE');
        ELSE
            FOR i IN 0 .. 4 LOOP add_row(v_layout, CHR(65 + i), 14, 'STANDARD'); END LOOP;
            FOR i IN 5 .. 11 LOOP add_row(v_layout, CHR(65 + i), 14, 'VIP'); END LOOP;
        END IF;
    END LOOP;
END;
/

-- 6. Phim, ban chieu, the loai, quyen phat hanh
DECLARE
    PROCEDURE add_film(
        p_code VARCHAR2, p_title VARCHAR2, p_orig VARCHAR2, p_syn VARCHAR2, p_runtime NUMBER,
        p_age VARCHAR2, p_status VARCHAR2, p_genres VARCHAR2, p_versions VARCHAR2,
        p_poster VARCHAR2, p_dist VARCHAR2, p_right_from_days NUMBER) IS
        v_film NUMBER;
        v_age NUMBER;
    BEGIN
        SELECT age_classification_id INTO v_age FROM age_classification WHERE classification_code = p_age;

        INSERT INTO film (film_code, title_local, title_original, synopsis, runtime_minutes,
                          age_classification_id, poster_uri, approval_status)
        VALUES (p_code, p_title, p_orig, p_syn, p_runtime, v_age,
                'https://placehold.co/400x600?text=' || p_poster, p_status)
        RETURNING film_id INTO v_film;

        FOR g IN (
            SELECT TRIM(REGEXP_SUBSTR(p_genres, '[^,]+', 1, LEVEL)) code
            FROM dual CONNECT BY REGEXP_SUBSTR(p_genres, '[^,]+', 1, LEVEL) IS NOT NULL
        ) LOOP
            INSERT INTO film_genre_map (film_id, genre_id)
            SELECT v_film, genre_id FROM film_genre WHERE genre_code = g.code;
        END LOOP;

        FOR v IN (
            SELECT TRIM(REGEXP_SUBSTR(p_versions, '[^,]+', 1, LEVEL)) code
            FROM dual CONNECT BY REGEXP_SUBSTR(p_versions, '[^,]+', 1, LEVEL) IS NOT NULL
        ) LOOP
            INSERT INTO film_version (film_id, version_code, language_code, subtitle_code, format_code, runtime_minutes)
            VALUES (v_film, v.code,
                    CASE WHEN v.code IN ('2D_DUB', '2D_VN') THEN 'vi' ELSE 'en' END,
                    CASE WHEN v.code IN ('2D_SUB', '3D_SUB') THEN 'vi' END,
                    CASE WHEN v.code LIKE '3D%' THEN '3D' ELSE '2D' END,
                    p_runtime);
        END LOOP;

        IF p_dist IS NOT NULL THEN
            INSERT INTO film_release_right (film_id, cinema_id, valid_from, valid_to, distributor_name, reference_code)
            VALUES (v_film, NULL, SYSTIMESTAMP + NUMTODSINTERVAL(p_right_from_days, 'DAY'),
                    SYSTIMESTAMP + NUMTODSINTERVAL(p_right_from_days + 150, 'DAY'), p_dist, 'HD-' || p_code);
        END IF;
    END;
BEGIN
    add_film('TF001', 'Hoàng Hôn Phía Biển', 'Where the Sea Sets',
        'Sau nhiều năm xa quê, cô giáo trẻ Minh Anh trở về làng chài miền Trung và gặp lại người bạn thời thơ ấu. Giữa những mùa bão, họ phải chọn giữa ước mơ rời đi và tình yêu gắn với biển.',
        112, 'T13', 'APPROVED', 'DRAMA,ROMANCE', '2D_VN', 'Hoang+Hon+Phia+Bien', 'Hãng phim Ngôi Sao', -30);
    add_film('TF002', 'Mật Mã Đen', 'Black Cipher',
        'Một chuyên gia giải mã bị cuốn vào vụ đánh cắp dữ liệu ngân hàng xuyên quốc gia. Mỗi manh mối tìm được lại đẩy cô đến gần hơn với kẻ đã phản bội đội điều tra.',
        128, 'T16', 'APPROVED', 'ACTION,THRILLER,CRIME', '2D_SUB,2D_DUB', 'Mat+Ma+Den', 'Hãng phim Bắc Cực', -30);
    add_film('TF003', 'Chuyến Tàu Cuối Đêm', 'Last Train Home',
        'Năm hành khách lỡ chuyến tàu cuối cùng về ngoại ô và phát hiện toa tàu không hề đi theo lộ trình quen thuộc. Càng về khuya, những điều họ giấu kín càng bị phơi bày.',
        104, 'T16', 'APPROVED', 'HORROR,THRILLER', '2D_SUB', 'Chuyen+Tau+Cuoi+Dem', 'Hãng phim Bắc Cực', -30);
    add_film('TF004', 'Vũ Trụ Nhỏ Của Bi', 'Bi''s Little Universe',
        'Cậu bé Bi tình cờ thu nhỏ cả khu vườn thành một vũ trụ riêng và kết bạn với những sinh vật tí hon. Cùng nhau, họ đi tìm viên đá ánh sáng để cứu khu vườn khỏi mùa đông kéo dài.',
        96, 'P', 'APPROVED', 'ANIMATION,FAMILY,ADVENTURE', '2D_DUB,2D_SUB', 'Vu+Tru+Nho+Cua+Bi', 'Hãng phim Cầu Vồng', -30);
    add_film('TF005', 'Ký Ức Mùa Hạ', 'Summer Memories',
        'Hai người bạn thân hẹn nhau trở lại ngôi trường cũ sau mười năm để mở chiếc hộp thời gian. Những lá thư bên trong làm sống lại một mùa hè mà cả hai đều muốn quên.',
        118, 'T13', 'APPROVED', 'ROMANCE,COMEDY,DRAMA', '2D_VN', 'Ky+Uc+Mua+Ha', 'Hãng phim Ngôi Sao', -30);
    add_film('TF006', 'Đại Chiến Thiên Hà: Hồi Sinh', 'Galaxy Rising',
        'Khi mạng lưới trạm năng lượng giữa các thiên hà bị tê liệt, phi hành đoàn tàu Aurora nhận nhiệm vụ cuối cùng để khởi động lại nguồn sống của hàng triệu hành tinh.',
        142, 'T13', 'APPROVED', 'SCIFI,ACTION,ADVENTURE', '2D_SUB,3D_SUB,2D_DUB', 'Dai+Chien+Thien+Ha', 'Hãng phim Bắc Cực', -30);
    add_film('TF007', 'Gia Đình Là Số Một', 'Family First',
        'Ba thế hệ cùng sống dưới một mái nhà phố cổ và mỗi người đều có một kế hoạch riêng cho dịp giỗ tổ. Mọi chuyện rối tung khi họ vô tình tổ chức hai bữa tiệc trong cùng một ngày.',
        101, 'P', 'APPROVED', 'COMEDY,FAMILY', '2D_VN', 'Gia+Dinh+La+So+Mot', 'Hãng phim Ngôi Sao', -30);
    add_film('TF008', 'Bóng Tối Trong Gương', 'The Dark Mirror',
        'Chiếc gương cổ được mua lại từ một cuộc đấu giá bắt đầu phản chiếu những điều chưa xảy ra. Nhà phục chế Hạ Vy phải tìm ra nguồn gốc của nó trước khi lời tiên đoán cuối cùng thành sự thật.',
        97, 'T18', 'APPROVED', 'HORROR,THRILLER', '2D_SUB', 'Bong+Toi+Trong+Guong', 'Hãng phim Bắc Cực', -30);
    add_film('TF009', 'Người Giữ Rừng', 'Keeper of the Forest',
        'Một kiểm lâm già và cô bé mồ côi cùng lên đường đưa chú voi con trở lại đàn giữa khu rừng đang bị đe dọa. Chuyến đi dạy cả hai về lòng tin và sự tử tế.',
        109, 'K', 'APPROVED', 'ADVENTURE,FAMILY,DRAMA', '2D_SUB,2D_DUB', 'Nguoi+Giu+Rung', 'Hãng phim Cầu Vồng', -30);
    add_film('TF010', 'Hành Trình Phương Nam', 'Journey South',
        'Ba anh em chia nhau tro cốt của cha và cùng xuôi về miền Tây theo đúng lộ trình ông từng đi thời trẻ. Trên những con sông, họ đối diện với bí mật gia đình suốt nhiều năm.',
        126, 'T13', 'APPROVED', 'DRAMA,ADVENTURE', '2D_VN', 'Hanh+Trinh+Phuong+Nam', 'Hãng phim Ngôi Sao', -30);
    add_film('TF011', 'Mùa Gió Chướng', 'Season of the Wind',
        'Cô gái trẻ nối nghiệp cha giữ ghe hàng trên sông Tiền và phải đối mặt với những thương lái muốn thâu tóm chợ nổi.',
        115, 'T13', 'APPROVED', 'DRAMA', '2D_VN', 'Mua+Gio+Chuong', 'Hãng phim Ngôi Sao', 8);
    add_film('TF012', 'Thám Tử Phố Cổ', 'The Old Quarter Detective',
        'Một thám tử về hưu nhận vụ mất tích bí ẩn của người bán tranh trên phố cổ Hà Nội, và lần ra chuỗi liên hệ với một vụ án cũ.',
        124, 'T16', 'APPROVED', 'CRIME,THRILLER', '2D_VN', 'Tham+Tu+Pho+Co', 'Hãng phim Bắc Cực', 10);
    add_film('TF013', 'Điệp Khúc Cuối Cùng', 'The Final Refrain',
        'Phim đang trong giai đoạn biên tập nội dung, chưa công bố lịch chiếu.',
        108, 'T13', 'DRAFT', 'DRAMA,ROMANCE', '2D_VN', 'Diep+Khuc+Cuoi+Cung', NULL, 0);
END;
/

-- 7. Bang gia ve (gia chua gom VAT, VAT 8 phan tram cong them khi tinh don, don vi VND)
-- Quy tac uu tien: so nho hon duoc ap dung truoc. Suat EARLY va rap Da Nang co gia rieng.
INSERT INTO price_rule (rule_code, cinema_id, room_type_id, seat_type_id, screening_type_code,
                        valid_from, base_price, priority_no, approval_status)
SELECT p.rule_code,
       (SELECT cinema_id FROM cinema WHERE cinema_code = p.cinema_code),
       (SELECT room_type_id FROM room_type WHERE type_code = p.room_type),
       (SELECT seat_type_id FROM seat_type WHERE type_code = p.seat_type),
       p.screening_type, SYSTIMESTAMP - INTERVAL '60' DAY, p.base_price, p.priority_no, 'APPROVED'
FROM (
    SELECT 'PR-ALL-STANDARD' rule_code, CAST(NULL AS VARCHAR2(20)) cinema_code, CAST(NULL AS VARCHAR2(30)) room_type,
           'STANDARD' seat_type, CAST(NULL AS VARCHAR2(40)) screening_type, 80000 base_price, 200 priority_no FROM dual UNION ALL
    SELECT 'PR-ALL-VIP', NULL, NULL, 'VIP', NULL, 100000, 200 FROM dual UNION ALL
    SELECT 'PR-ALL-COUPLE', NULL, NULL, 'COUPLE', NULL, 180000, 200 FROM dual UNION ALL
    SELECT 'PR-PREMIUM-VIP', NULL, 'PREMIUM', 'VIP', NULL, 140000, 100 FROM dual UNION ALL
    SELECT 'PR-PREMIUM-COUPLE', NULL, 'PREMIUM', 'COUPLE', NULL, 260000, 100 FROM dual UNION ALL
    SELECT 'PR-LARGE-STANDARD', NULL, 'LARGE_FORMAT', 'STANDARD', NULL, 110000, 100 FROM dual UNION ALL
    SELECT 'PR-LARGE-VIP', NULL, 'LARGE_FORMAT', 'VIP', NULL, 135000, 100 FROM dual UNION ALL
    SELECT 'PR-DN-STANDARD', 'DN-HC', 'STANDARD', 'STANDARD', NULL, 70000, 90 FROM dual UNION ALL
    SELECT 'PR-DN-VIP', 'DN-HC', 'STANDARD', 'VIP', NULL, 90000, 90 FROM dual UNION ALL
    SELECT 'PR-EARLY-STANDARD', NULL, 'STANDARD', 'STANDARD', 'EARLY', 60000, 50 FROM dual UNION ALL
    SELECT 'PR-EARLY-VIP', NULL, 'STANDARD', 'VIP', 'EARLY', 80000, 50 FROM dual
) p;

-- 8. Suat chieu 7 ngay toi va ghe tung suat
DECLARE
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    TYPE t_slots IS VARRAY(5) OF NUMBER;
    v_versions t_ids;
    v_n        PLS_INTEGER := 0;
    v_slots    t_slots := t_slots(9.5, 12.5, 15.5, 18.5, 21.5);
    v_base     DATE := TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE));
    v_idx      PLS_INTEGER := 0;
    v_pick     PLS_INTEGER;
    v_start    TIMESTAMP WITH TIME ZONE;
    v_end      TIMESTAMP WITH TIME ZONE;
    v_sid      NUMBER;
    v_film     NUMBER;
    v_runtime  NUMBER;
    v_format   VARCHAR2(40);
    v_type     VARCHAR2(40);
BEGIN
    FOR v IN (
        SELECT fv.film_version_id
        FROM film_version fv
        JOIN film f ON f.film_id = fv.film_id
        WHERE f.film_code IN ('TF001', 'TF002', 'TF003', 'TF004', 'TF005', 'TF006', 'TF007', 'TF008', 'TF009', 'TF010')
        ORDER BY fv.film_version_id
    ) LOOP
        v_n := v_n + 1;
        v_versions(v_n) := v.film_version_id;
    END LOOP;

    FOR r IN (
        SELECT cr.room_id, cr.cinema_id, l.layout_id, rt.type_code, rt.type_name
        FROM cinema_room cr
        JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
        JOIN room_type rt ON rt.room_type_id = cr.room_type_id
        JOIN room_layout l ON l.room_id = cr.room_id AND l.status = 'ACTIVE'
        ORDER BY cr.room_id
    ) LOOP
        v_idx := v_idx + 1;
        FOR d IN 0 .. 6 LOOP
            FOR s IN 1 .. 5 LOOP
                v_start := FROM_TZ(CAST(v_base + d + v_slots(s) / 24 AS TIMESTAMP), 'Asia/Ho_Chi_Minh');
                CONTINUE WHEN v_start <= SYSTIMESTAMP + INTERVAL '1' HOUR;

                v_pick := MOD(v_idx * 5 + d * 3 + s * 2, v_n) + 1;
                SELECT film_id, runtime_minutes, format_code
                INTO v_film, v_runtime, v_format
                FROM film_version WHERE film_version_id = v_versions(v_pick);

                v_end := v_start + NUMTODSINTERVAL(v_runtime, 'MINUTE');
                v_type := CASE WHEN s = 1 THEN 'EARLY' ELSE 'STANDARD' END;

                INSERT INTO screening (cinema_id, room_id, layout_id, film_id, film_version_id,
                                       room_type_code_snapshot, room_type_name_snapshot, format_code,
                                       screening_type_code, starts_at, ends_at, cleanup_minutes, status)
                VALUES (r.cinema_id, r.room_id, r.layout_id, v_film, v_versions(v_pick),
                        r.type_code, r.type_name, v_format, v_type, v_start, v_end, 15, 'ON_SALE')
                RETURNING screening_id INTO v_sid;

                INSERT INTO show_seat (screening_id, room_seat_id, seat_status, seat_code_snapshot,
                                       row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
                SELECT v_sid, rs.room_seat_id, CASE rs.sellable WHEN 'Y' THEN 'AVAILABLE' ELSE 'BLOCKED' END,
                       rs.seat_code, rs.row_label, rs.seat_label, st.type_code
                FROM room_seat rs
                JOIN seat_type st ON st.seat_type_id = rs.seat_type_id
                WHERE rs.layout_id = r.layout_id AND rs.status = 'ACTIVE';
            END LOOP;
        END LOOP;
    END LOOP;
END;
/

-- Suat sap chieu (SCHEDULED, chua mo ban) cho hai phim co quyen phat hanh bat dau sau 7 ngay
DECLARE
    v_base  DATE := TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE));
    v_start TIMESTAMP WITH TIME ZONE;
    v_end   TIMESTAMP WITH TIME ZONE;
    v_sid   NUMBER;
BEGIN
    FOR x IN (
        SELECT 'TF011' film_code, 'P01' room_code, 8 day_offset FROM dual UNION ALL
        SELECT 'TF011', 'P02', 9 FROM dual UNION ALL
        SELECT 'TF012', 'P01', 10 FROM dual UNION ALL
        SELECT 'TF012', 'P02', 11 FROM dual
    ) LOOP
        FOR r IN (
            SELECT cr.room_id, cr.cinema_id, l.layout_id, rt.type_code, rt.type_name,
                   fv.film_version_id, fv.film_id, fv.runtime_minutes, fv.format_code
            FROM cinema_room cr
            JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code = 'HCM-Q1'
            JOIN room_type rt ON rt.room_type_id = cr.room_type_id
            JOIN room_layout l ON l.room_id = cr.room_id AND l.status = 'ACTIVE'
            JOIN film f ON f.film_code = x.film_code
            JOIN film_version fv ON fv.film_id = f.film_id
            WHERE cr.room_code = x.room_code
        ) LOOP
            v_start := FROM_TZ(CAST(v_base + x.day_offset + 19 / 24 AS TIMESTAMP), 'Asia/Ho_Chi_Minh');
            v_end := v_start + NUMTODSINTERVAL(r.runtime_minutes, 'MINUTE');

            INSERT INTO screening (cinema_id, room_id, layout_id, film_id, film_version_id,
                                   room_type_code_snapshot, room_type_name_snapshot, format_code,
                                   screening_type_code, starts_at, ends_at, cleanup_minutes, status)
            VALUES (r.cinema_id, r.room_id, r.layout_id, r.film_id, r.film_version_id,
                    r.type_code, r.type_name, r.format_code, 'STANDARD', v_start, v_end, 15, 'SCHEDULED')
            RETURNING screening_id INTO v_sid;

            INSERT INTO show_seat (screening_id, room_seat_id, seat_status, seat_code_snapshot,
                                   row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
            SELECT v_sid, rs.room_seat_id, CASE rs.sellable WHEN 'Y' THEN 'AVAILABLE' ELSE 'BLOCKED' END,
                   rs.seat_code, rs.row_label, rs.seat_label, st.type_code
            FROM room_seat rs
            JOIN seat_type st ON st.seat_type_id = rs.seat_type_id
            WHERE rs.layout_id = r.layout_id AND rs.status = 'ACTIVE';
        END LOOP;
    END LOOP;
END;
/

-- 9. Khuyen mai va voucher
INSERT INTO promo_campaign (campaign_code, campaign_name, discount_type, discount_value, max_discount,
                            budget_amount, usage_limit, per_customer_limit, valid_from, valid_to, stackable, status)
VALUES ('WELCOME10', 'Chào bạn mới - giảm 10%', 'PERCENT', 10, 50000, 50000000, 1000, 1,
        SYSTIMESTAMP - INTERVAL '30' DAY, SYSTIMESTAMP + INTERVAL '180' DAY(3), 'N', 'ACTIVE');
INSERT INTO promo_campaign (campaign_code, campaign_name, discount_type, discount_value, max_discount,
                            budget_amount, usage_limit, per_customer_limit, valid_from, valid_to, stackable, status)
VALUES ('STUDENT15', 'Ưu đãi học sinh sinh viên - giảm 15%', 'PERCENT', 15, 40000, 30000000, 500, 2,
        SYSTIMESTAMP - INTERVAL '30' DAY, SYSTIMESTAMP + INTERVAL '90' DAY, 'N', 'ACTIVE');
INSERT INTO promo_campaign (campaign_code, campaign_name, discount_type, discount_value, max_discount,
                            budget_amount, usage_limit, per_customer_limit, valid_from, valid_to, stackable, status)
VALUES ('WEEKDAY30K', 'Giảm 30.000đ vé ngày thường', 'FIXED', 30000, NULL, 20000000, 600, 1,
        SYSTIMESTAMP - INTERVAL '30' DAY, SYSTIMESTAMP + INTERVAL '60' DAY, 'N', 'PAUSED');

BEGIN
    FOR n IN 1 .. 20 LOOP
        INSERT INTO promo_voucher (promo_campaign_id, voucher_code)
        SELECT promo_campaign_id, 'WELCOME10-' || LPAD(n, 4, '0') FROM promo_campaign WHERE campaign_code = 'WELCOME10';
    END LOOP;
    FOR n IN 1 .. 10 LOOP
        INSERT INTO promo_voucher (promo_campaign_id, voucher_code)
        SELECT promo_campaign_id, 'STUDENT15-' || LPAD(n, 4, '0') FROM promo_campaign WHERE campaign_code = 'STUDENT15';
    END LOOP;
END;
/

-- 10. Quay do an thuc uong
INSERT INTO fnb_category (category_code, category_name) VALUES ('POPCORN', 'Bắp rang');
INSERT INTO fnb_category (category_code, category_name) VALUES ('DRINK', 'Nước uống');
INSERT INTO fnb_category (category_code, category_name) VALUES ('SNACK', 'Đồ ăn nhẹ');
INSERT INTO fnb_category (category_code, category_name) VALUES ('COMBO', 'Combo');

INSERT INTO fnb_product (fnb_category_id, product_code, product_name, product_type, unit_code, track_inventory)
SELECT c.fnb_category_id, p.product_code, p.product_name, p.product_type, p.unit_code, p.track_inventory
FROM (
    SELECT 'POPCORN' category_code, 'POPCORN_SWEET_M' product_code, 'Bắp ngọt size vừa' product_name,
           'ITEM' product_type, 'PHAN' unit_code, 'Y' track_inventory FROM dual UNION ALL
    SELECT 'POPCORN', 'POPCORN_SWEET_L', 'Bắp ngọt size lớn', 'ITEM', 'PHAN', 'Y' FROM dual UNION ALL
    SELECT 'POPCORN', 'POPCORN_CARAMEL_L', 'Bắp caramel size lớn', 'ITEM', 'PHAN', 'Y' FROM dual UNION ALL
    SELECT 'DRINK', 'SODA_M', 'Nước ngọt có ga size vừa', 'ITEM', 'LY', 'Y' FROM dual UNION ALL
    SELECT 'DRINK', 'SODA_L', 'Nước ngọt có ga size lớn', 'ITEM', 'LY', 'Y' FROM dual UNION ALL
    SELECT 'DRINK', 'WATER_500', 'Nước suối 500ml', 'ITEM', 'CHAI', 'Y' FROM dual UNION ALL
    SELECT 'SNACK', 'NACHOS_CHEESE', 'Nachos phô mai', 'ITEM', 'PHAN', 'Y' FROM dual UNION ALL
    SELECT 'COMBO', 'COMBO_SOLO', 'Combo 1 người (1 bắp vừa + 1 nước vừa)', 'COMBO', 'COMBO', 'N' FROM dual UNION ALL
    SELECT 'COMBO', 'COMBO_COUPLE', 'Combo 2 người (1 bắp lớn + 2 nước vừa)', 'COMBO', 'COMBO', 'N' FROM dual UNION ALL
    SELECT 'COMBO', 'COMBO_FAMILY', 'Combo gia đình (2 bắp lớn + 4 nước vừa + 1 nachos)', 'COMBO', 'COMBO', 'N' FROM dual
) p
JOIN fnb_category c ON c.category_code = p.category_code;

INSERT INTO fnb_recipe_component (combo_product_id, component_product_id, quantity)
SELECT (SELECT fnb_product_id FROM fnb_product WHERE product_code = r.combo),
       (SELECT fnb_product_id FROM fnb_product WHERE product_code = r.component), r.qty
FROM (
    SELECT 'COMBO_SOLO' combo, 'POPCORN_SWEET_M' component, 1 qty FROM dual UNION ALL
    SELECT 'COMBO_SOLO', 'SODA_M', 1 FROM dual UNION ALL
    SELECT 'COMBO_COUPLE', 'POPCORN_SWEET_L', 1 FROM dual UNION ALL
    SELECT 'COMBO_COUPLE', 'SODA_M', 2 FROM dual UNION ALL
    SELECT 'COMBO_FAMILY', 'POPCORN_SWEET_L', 2 FROM dual UNION ALL
    SELECT 'COMBO_FAMILY', 'SODA_M', 4 FROM dual UNION ALL
    SELECT 'COMBO_FAMILY', 'NACHOS_CHEESE', 1 FROM dual
) r;

-- Gia ban moi rap; Da Nang thap hon 5 phan tram, lam tron den 1.000 dong
INSERT INTO fnb_product_price (fnb_product_id, cinema_id, valid_from, unit_price, approval_status)
SELECT p.fnb_product_id, c.cinema_id, SYSTIMESTAMP - INTERVAL '60' DAY,
       CASE WHEN c.cinema_code = 'DN-HC' THEN ROUND(b.base_price * 0.95 / 1000) * 1000 ELSE b.base_price END,
       'APPROVED'
FROM (
    SELECT 'POPCORN_SWEET_M' product_code, 55000 base_price FROM dual UNION ALL
    SELECT 'POPCORN_SWEET_L', 69000 FROM dual UNION ALL
    SELECT 'POPCORN_CARAMEL_L', 79000 FROM dual UNION ALL
    SELECT 'SODA_M', 35000 FROM dual UNION ALL
    SELECT 'SODA_L', 42000 FROM dual UNION ALL
    SELECT 'WATER_500', 20000 FROM dual UNION ALL
    SELECT 'NACHOS_CHEESE', 59000 FROM dual UNION ALL
    SELECT 'COMBO_SOLO', 85000 FROM dual UNION ALL
    SELECT 'COMBO_COUPLE', 159000 FROM dual UNION ALL
    SELECT 'COMBO_FAMILY', 249000 FROM dual
) b
JOIN fnb_product p ON p.product_code = b.product_code
JOIN cinema c ON c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC');

-- 11. Ho so phap nhan (thong tin dung de in hoa don)
INSERT INTO legal_entity_profile (legal_entity_id, version_no, valid_from, display_name, tax_identifier,
                                  registered_address, invoice_email, source_note)
SELECT e.legal_entity_id, 1, SYSTIMESTAMP - INTERVAL '365' DAY(3), e.legal_name, e.tax_identifier,
       p.address, p.email, 'Du lieu mau phuc vu demo'
FROM legal_entity e
JOIN (
    SELECT 'LE-HCM' entity_code, '88 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh' address,
           'ketoan.hcm@thefilm.example' email FROM dual UNION ALL
    SELECT 'LE-HN', '25 Phố Huế, Phường Phạm Đình Hổ, Quận Hai Bà Trưng, Hà Nội', 'ketoan.hn@thefilm.example' FROM dual
) p ON p.entity_code = e.entity_code;

-- 12. Phong ban, chuc danh, ca lam viec mau
INSERT INTO department (chain_id, department_code, department_name)
SELECT c.chain_id, d.code, d.name
FROM org_chain c
CROSS JOIN (
    SELECT 'MGMT' code, 'Ban điều hành' name FROM dual UNION ALL
    SELECT 'HR', 'Phòng Nhân sự' FROM dual UNION ALL
    SELECT 'FIN', 'Phòng Kế toán' FROM dual UNION ALL
    SELECT 'CS', 'Chăm sóc khách hàng' FROM dual UNION ALL
    SELECT 'OPS', 'Vận hành rạp' FROM dual UNION ALL
    SELECT 'FNB', 'Quầy thức ăn và đồ uống' FROM dual
) d
WHERE c.chain_code = 'THEFILM';

INSERT INTO job_position (chain_id, position_code, position_name)
SELECT c.chain_id, p.code, p.name
FROM org_chain c
CROSS JOIN (
    SELECT 'GM' code, 'Giám đốc vận hành chuỗi' name FROM dual UNION ALL
    SELECT 'HR_MGR', 'Trưởng phòng Nhân sự' FROM dual UNION ALL
    SELECT 'ACCOUNTANT', 'Kế toán tổng hợp' FROM dual UNION ALL
    SELECT 'CS_LEAD', 'Trưởng nhóm chăm sóc khách hàng' FROM dual UNION ALL
    SELECT 'CS_AGENT', 'Nhân viên chăm sóc khách hàng' FROM dual UNION ALL
    SELECT 'CINEMA_MGR', 'Quản lý rạp' FROM dual UNION ALL
    SELECT 'CASHIER', 'Nhân viên bán vé' FROM dual UNION ALL
    SELECT 'USHER', 'Nhân viên soát vé' FROM dual UNION ALL
    SELECT 'CONCESSION', 'Nhân viên quầy F&B' FROM dual
) p
WHERE c.chain_code = 'THEFILM';

INSERT INTO shift_template (cinema_id, shift_code, shift_name, start_time, end_time, unpaid_break_minutes)
SELECT c.cinema_id, s.code, s.name, s.st, s.et, 30
FROM cinema c
CROSS JOIN (
    SELECT 'CA_SANG' code, 'Ca sáng' name, '08:00' st, '16:00' et FROM dual UNION ALL
    SELECT 'CA_TOI', 'Ca tối', '15:00', '23:00' FROM dual
) s
WHERE c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC');

INSERT INTO leave_type (leave_code, leave_name, paid_flag)
SELECT code, name, paid FROM (
    SELECT 'ANNUAL' code, 'Nghỉ phép năm' name, 'Y' paid FROM dual UNION ALL
    SELECT 'SICK', 'Nghỉ ốm có giấy bác sĩ', 'Y' FROM dual UNION ALL
    SELECT 'PERSONAL', 'Nghỉ việc riêng có lương', 'Y' FROM dual UNION ALL
    SELECT 'UNPAID', 'Nghỉ không lương', 'N' FROM dual
);

-- 13. Tai khoan nhan vien va nhan vien
-- Tai khoan chi luu dinh danh OIDC (issuer, subject), khong luu mat khau. Subject la ma bam co dinh
-- tu ma nhan vien de du lieu mau lap lai duoc; khi dung Keycloak that thi cap nhat subject theo user that.
-- Nhan vien khong co tai khoan (has_account = 'N') la nhan vien chua duoc cap quyen dang nhap.
DECLARE
    c_issuer CONSTANT VARCHAR2(200) := 'http://localhost:8081/realms/thefilm';
    v_chain  NUMBER;
    v_user   NUMBER;
    v_n      PLS_INTEGER := 0;
    v_subj   VARCHAR2(64);

    FUNCTION subj(p_key VARCHAR2) RETURN VARCHAR2 IS
        h VARCHAR2(64);
    BEGIN
        SELECT LOWER(RAWTOHEX(STANDARD_HASH('thefilm:' || p_key, 'SHA256'))) INTO h FROM dual;
        RETURN SUBSTR(h, 1, 8) || '-' || SUBSTR(h, 9, 4) || '-' || SUBSTR(h, 13, 4) || '-'
            || SUBSTR(h, 17, 4) || '-' || SUBSTR(h, 21, 12);
    END;
BEGIN
    SELECT chain_id INTO v_chain FROM org_chain WHERE chain_code = 'THEFILM';

    FOR s IN (
        SELECT 'NV0001' code, 'Nguyễn Quốc Khánh' name, DATE '1980-03-14' dob, 'GM' pos, 'MGMT' dept, CAST(NULL AS VARCHAR2(20)) cinema,
               'khanh.nguyen@thefilm.example' email, 60000000 salary, 'Y' acct, DATE '2019-01-02' hired FROM dual UNION ALL
        SELECT 'NV0002', 'Trần Thị Mai Hương', DATE '1987-07-22', 'HR_MGR', 'HR', NULL, 'huong.tran@thefilm.example', 28000000, 'Y', DATE '2019-03-15' FROM dual UNION ALL
        SELECT 'NV0003', 'Lê Văn Phúc', DATE '1985-11-05', 'ACCOUNTANT', 'FIN', NULL, 'phuc.le@thefilm.example', 25000000, 'Y', DATE '2019-06-01' FROM dual UNION ALL
        SELECT 'NV0004', 'Phạm Thu Trang', DATE '1990-02-18', 'CS_LEAD', 'CS', NULL, 'trang.pham@thefilm.example', 22000000, 'Y', DATE '2020-02-10' FROM dual UNION ALL
        SELECT 'NV0005', 'Đỗ Hải Yến', DATE '1996-09-09', 'CS_AGENT', 'CS', NULL, 'yen.do@thefilm.example', 12000000, 'Y', DATE '2022-05-23' FROM dual UNION ALL
        SELECT 'NV0101', 'Võ Minh Tuấn', DATE '1988-05-12', 'CINEMA_MGR', 'OPS', 'HCM-Q1', 'tuan.vo@thefilm.example', 28000000, 'Y', DATE '2019-04-01' FROM dual UNION ALL
        SELECT 'NV0102', 'Nguyễn Thị Lan', DATE '1999-01-20', 'CASHIER', 'OPS', 'HCM-Q1', 'lan.nguyen@thefilm.example', 9000000, 'Y', DATE '2022-08-01' FROM dual UNION ALL
        SELECT 'NV0103', 'Hoàng Gia Bảo', DATE '2000-06-30', 'CASHIER', 'OPS', 'HCM-Q1', 'bao.hoang@thefilm.example', 9000000, 'Y', DATE '2023-02-13' FROM dual UNION ALL
        SELECT 'NV0104', 'Lý Thanh Hà', DATE '2002-10-11', 'USHER', 'OPS', 'HCM-Q1', 'ha.ly@thefilm.example', 8000000, 'N', DATE '2025-09-01' FROM dual UNION ALL
        SELECT 'NV0105', 'Bùi Quang Huy', DATE '1997-12-03', 'CONCESSION', 'FNB', 'HCM-Q1', 'huy.bui@thefilm.example', 8500000, 'Y', DATE '2023-07-17' FROM dual UNION ALL
        SELECT 'NV0201', 'Đặng Thị Hồng Nhung', DATE '1989-08-03', 'CINEMA_MGR', 'OPS', 'HCM-TD', 'nhung.dang@thefilm.example', 28000000, 'Y', DATE '2020-01-06' FROM dual UNION ALL
        SELECT 'NV0202', 'Trương Quốc Việt', DATE '1998-04-25', 'CASHIER', 'OPS', 'HCM-TD', 'viet.truong@thefilm.example', 9000000, 'Y', DATE '2022-10-03' FROM dual UNION ALL
        SELECT 'NV0203', 'Phan Thị Kim Ngân', DATE '2001-02-14', 'CASHIER', 'OPS', 'HCM-TD', 'ngan.phan@thefilm.example', 9000000, 'Y', DATE '2024-01-15' FROM dual UNION ALL
        SELECT 'NV0204', 'Ngô Đức Thịnh', DATE '2000-09-17', 'USHER', 'OPS', 'HCM-TD', 'thinh.ngo@thefilm.example', 8000000, 'Y', DATE '2024-03-04' FROM dual UNION ALL
        SELECT 'NV0205', 'Dương Thị Cẩm Tú', DATE '1999-07-08', 'CONCESSION', 'FNB', 'HCM-TD', 'tu.duong@thefilm.example', 8500000, 'Y', DATE '2023-11-20' FROM dual UNION ALL
        SELECT 'NV0301', 'Vũ Đình Long', DATE '1986-12-29', 'CINEMA_MGR', 'OPS', 'HN-CG', 'long.vu@thefilm.example', 28000000, 'Y', DATE '2020-06-15' FROM dual UNION ALL
        SELECT 'NV0302', 'Nguyễn Thị Thu Hà', DATE '1998-03-06', 'CASHIER', 'OPS', 'HN-CG', 'thuha.nguyen@thefilm.example', 9000000, 'Y', DATE '2022-09-12' FROM dual UNION ALL
        SELECT 'NV0303', 'Trần Văn Nam', DATE '1999-11-23', 'CASHIER', 'OPS', 'HN-CG', 'nam.tran@thefilm.example', 9000000, 'Y', DATE '2023-05-08' FROM dual UNION ALL
        SELECT 'NV0304', 'Lê Hoàng Anh', DATE '2003-01-31', 'USHER', 'OPS', 'HN-CG', 'hoanganh.le@thefilm.example', 8000000, 'N', DATE '2025-10-06' FROM dual UNION ALL
        SELECT 'NV0305', 'Phạm Thị Ngọc Ánh', DATE '2000-05-19', 'CONCESSION', 'FNB', 'HN-CG', 'ngocanh.pham@thefilm.example', 8500000, 'Y', DATE '2024-02-19' FROM dual UNION ALL
        SELECT 'NV0401', 'Huỳnh Văn Thành', DATE '1987-10-07', 'CINEMA_MGR', 'OPS', 'DN-HC', 'thanh.huynh@thefilm.example', 28000000, 'Y', DATE '2021-03-01' FROM dual UNION ALL
        SELECT 'NV0402', 'Trần Thị Bích Ngọc', DATE '1999-08-12', 'CASHIER', 'OPS', 'DN-HC', 'bichngoc.tran@thefilm.example', 9000000, 'Y', DATE '2022-12-05' FROM dual UNION ALL
        SELECT 'NV0403', 'Lê Công Danh', DATE '2001-06-02', 'CASHIER', 'OPS', 'DN-HC', 'danh.le@thefilm.example', 9000000, 'Y', DATE '2024-04-22' FROM dual UNION ALL
        SELECT 'NV0404', 'Nguyễn Hữu Phước', DATE '2002-03-27', 'USHER', 'OPS', 'DN-HC', 'phuoc.nguyen@thefilm.example', 8000000, 'Y', DATE '2024-08-12' FROM dual UNION ALL
        SELECT 'NV0405', 'Đoàn Thị Mỹ Linh', DATE '2000-12-15', 'CONCESSION', 'FNB', 'DN-HC', 'mylinh.doan@thefilm.example', 8500000, 'Y', DATE '2023-09-25' FROM dual
        ORDER BY 1
    ) LOOP
        v_n := v_n + 1;
        v_user := NULL;

        IF s.acct = 'Y' THEN
            v_subj := subj(s.code);
            INSERT INTO app_user (oidc_issuer, oidc_subject, display_name, email, phone)
            VALUES (c_issuer, v_subj, s.name, s.email, '0903' || LPAD(v_n, 6, '0'))
            RETURNING app_user_id INTO v_user;

            IF s.cinema IS NOT NULL THEN
                INSERT INTO user_cinema_scope (app_user_id, cinema_id, valid_from)
                SELECT v_user, cinema_id, SYSTIMESTAMP - INTERVAL '365' DAY(3) FROM cinema WHERE cinema_code = s.cinema;
            END IF;
        END IF;

        INSERT INTO employee (chain_id, app_user_id, employee_code, full_name, date_of_birth, phone, email,
                              status, hired_on)
        VALUES (v_chain, v_user, s.code, s.name, s.dob, '0903' || LPAD(v_n, 6, '0'), s.email, 'ACTIVE', s.hired);

        -- Bo nhiem tai rap (hoac khoi van phong) va hop dong lao dong
        INSERT INTO employee_assignment (employee_id, cinema_id, department_id, job_position_id, valid_from)
        SELECT e.employee_id,
               NVL((SELECT cinema_id FROM cinema WHERE cinema_code = s.cinema),
                   (SELECT cinema_id FROM cinema WHERE cinema_code = 'HCM-Q1')),
               (SELECT department_id FROM department WHERE chain_id = v_chain AND department_code = s.dept),
               (SELECT job_position_id FROM job_position WHERE chain_id = v_chain AND position_code = s.pos),
               s.hired
        FROM employee e WHERE e.chain_id = v_chain AND e.employee_code = s.code;

        INSERT INTO employee_contract (employee_id, contract_code, contract_type, valid_from, valid_to,
                                       base_salary, contract_status)
        SELECT e.employee_id, 'HD-' || s.code,
               CASE WHEN s.pos IN ('CASHIER', 'USHER', 'CONCESSION') THEN 'FIXED_TERM' ELSE 'INDEFINITE' END,
               s.hired,
               CASE WHEN s.pos IN ('CASHIER', 'USHER', 'CONCESSION') THEN ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 14) END,
               s.salary, 'ACTIVE'
        FROM employee e WHERE e.chain_id = v_chain AND e.employee_code = s.code;
    END LOOP;
END;
/

-- 14. Khach hang, dong y xu ly du lieu, phim yeu thich
DECLARE
    c_issuer CONSTANT VARCHAR2(200) := 'http://localhost:8081/realms/thefilm';
    v_user   NUMBER;
    v_n      PLS_INTEGER := 0;
    v_subj   VARCHAR2(64);

    FUNCTION subj(p_key VARCHAR2) RETURN VARCHAR2 IS
        h VARCHAR2(64);
    BEGIN
        SELECT LOWER(RAWTOHEX(STANDARD_HASH('thefilm:' || p_key, 'SHA256'))) INTO h FROM dual;
        RETURN SUBSTR(h, 1, 8) || '-' || SUBSTR(h, 9, 4) || '-' || SUBSTR(h, 13, 4) || '-'
            || SUBSTR(h, 17, 4) || '-' || SUBSTR(h, 21, 12);
    END;
BEGIN
    FOR c IN (
        SELECT 'KH0001' code, 'Nguyễn Thị Thanh Mai' name, 'thanhmai.nguyen@example.com' email, DATE '1996-04-12' dob, 'HCM-Q1' cinema, 'N' no_member FROM dual UNION ALL
        SELECT 'KH0002', 'Trần Văn Hùng', 'hung.tran92@example.com', DATE '1992-09-23', 'HCM-Q1', 'N' FROM dual UNION ALL
        SELECT 'KH0003', 'Lê Thị Bích Phượng', 'bichphuong.le@example.com', DATE '2001-12-05', 'HCM-TD', 'N' FROM dual UNION ALL
        SELECT 'KH0004', 'Phạm Quang Vinh', 'vinh.pham88@example.com', DATE '1988-02-17', 'HCM-TD', 'N' FROM dual UNION ALL
        SELECT 'KH0005', 'Hoàng Thị Thu Hiền', 'thuhien.hoang@example.com', DATE '1995-07-30', 'HN-CG', 'N' FROM dual UNION ALL
        SELECT 'KH0006', 'Đỗ Minh Quân', 'quan.do03@example.com', DATE '2003-03-08', 'HN-CG', 'N' FROM dual UNION ALL
        SELECT 'KH0007', 'Vũ Thị Hồng Nhung', 'hongnhung.vu@example.com', DATE '1990-10-19', 'DN-HC', 'N' FROM dual UNION ALL
        SELECT 'KH0008', 'Bùi Anh Tuấn', 'anhtuan.bui@example.com', DATE '1985-06-27', 'DN-HC', 'N' FROM dual UNION ALL
        SELECT 'KH0009', 'Ngô Thị Phương Thảo', 'phuongthao.ngo@example.com', DATE '1998-01-14', 'HCM-Q1', 'N' FROM dual UNION ALL
        SELECT 'KH0010', 'Đặng Hoàng Long', 'long.dang00@example.com', DATE '2000-11-02', 'HN-CG', 'N' FROM dual UNION ALL
        SELECT 'KH0011', 'Phan Thị Ngọc Diệp', 'ngocdiep.phan@example.com', DATE '1994-05-21', 'HCM-TD', 'Y' FROM dual UNION ALL
        SELECT 'KH0012', 'Trịnh Văn Khoa', 'khoa.trinh93@example.com', DATE '1993-08-15', 'DN-HC', 'Y' FROM dual UNION ALL
        SELECT 'KH0013', 'Lương Thị Mỹ Duyên', 'myduyen.luong@example.com', DATE '2002-04-09', 'HCM-Q1', 'Y' FROM dual UNION ALL
        SELECT 'KH0014', 'Võ Thanh Sơn', 'thanhson.vo@example.com', DATE '1987-12-25', 'HN-CG', 'Y' FROM dual
        ORDER BY 1
    ) LOOP
        v_n := v_n + 1;

        v_subj := subj(c.code);
        INSERT INTO app_user (oidc_issuer, oidc_subject, display_name, email, phone)
        VALUES (c_issuer, v_subj, c.name, c.email, '0912' || LPAD(v_n, 6, '0'))
        RETURNING app_user_id INTO v_user;

        INSERT INTO customer_profile (app_user_id, preferred_cinema_id, date_of_birth)
        SELECT v_user, cinema_id, c.dob FROM cinema WHERE cinema_code = c.cinema;

        INSERT INTO customer_consent (app_user_id, consent_type, policy_version, consent_status, captured_channel, captured_at)
        VALUES (v_user, 'TERMS', '2026.1', 'GRANTED', 'WEB', SYSTIMESTAMP - NUMTODSINTERVAL(200 - v_n * 7, 'DAY'));
        INSERT INTO customer_consent (app_user_id, consent_type, policy_version, consent_status, captured_channel, captured_at)
        VALUES (v_user, 'PRIVACY', '2026.1', 'GRANTED', 'WEB', SYSTIMESTAMP - NUMTODSINTERVAL(200 - v_n * 7, 'DAY'));
        INSERT INTO customer_consent (app_user_id, consent_type, policy_version, consent_status, captured_channel, captured_at)
        VALUES (v_user, 'MARKETING', '2026.1',
                CASE WHEN MOD(v_n, 3) = 0 THEN 'DECLINED' WHEN v_n = 5 THEN 'WITHDRAWN' ELSE 'GRANTED' END,
                'WEB', SYSTIMESTAMP - NUMTODSINTERVAL(200 - v_n * 7, 'DAY'));

        INSERT INTO user_favorite_film (app_user_id, film_id)
        SELECT v_user, film_id FROM film WHERE film_code = 'TF' || LPAD(MOD(v_n, 10) + 1, 3, '0');
        INSERT INTO user_favorite_film (app_user_id, film_id)
        SELECT v_user, film_id FROM film WHERE film_code = 'TF' || LPAD(MOD(v_n + 4, 10) + 1, 3, '0');

        -- Hoi vien (khach KH0011 tro di chua dang ky the thanh vien)
        IF c.no_member = 'N' THEN
            INSERT INTO member_account (app_user_id, member_number, joined_at)
            VALUES (v_user, 'TFM' || LPAD(100000 + v_n, 8, '0'), SYSTIMESTAMP - NUMTODSINTERVAL(180 - v_n * 9, 'DAY'));
        END IF;
    END LOOP;
END;
/

-- 15. Hang thanh vien (diem tich luy: 1 diem cho moi 10.000 dong)
INSERT INTO member_tier (tier_code, tier_name, minimum_points, benefits_json)
SELECT code, name, pts, benefits FROM (
    SELECT 'BRONZE' code, 'Đồng' name, 0 pts, '{"earnRate":1,"birthdayGift":false}' benefits FROM dual UNION ALL
    SELECT 'SILVER', 'Bạc', 100, '{"earnRate":1,"birthdayGift":true}' FROM dual UNION ALL
    SELECT 'GOLD', 'Vàng', 300, '{"earnRate":1.2,"birthdayGift":true,"freePopcornPerQuarter":1}' FROM dual UNION ALL
    SELECT 'PLATINUM', 'Bạch kim', 800, '{"earnRate":1.5,"birthdayGift":true,"freePopcornPerQuarter":2,"priorityHotline":true}' FROM dual
);

-- 16. Thue gia lap (chi de demo, simulation_only = Y)
INSERT INTO tax_category (category_code, category_name, description)
VALUES ('TICKET', 'Vé xem phim', 'Dịch vụ chiếu phim, giá vé chưa gồm VAT');
INSERT INTO tax_category (category_code, category_name, description)
VALUES ('FNB', 'Thức ăn và đồ uống', 'Bắp rang, nước uống, combo bán tại quầy');

INSERT INTO tax_rule (tax_category_id, rule_code, tax_name, tax_type, calculation_method, rate_percent,
                      valid_from, valid_to, source_reference, approval_status)
SELECT c.tax_category_id, r.rule_code, r.tax_name, 'VAT', 'EXCLUSIVE', r.rate, r.vf, r.vt,
       'Gia lap phuc vu demo, can ke toan xac nhan truoc khi dung that', r.st
FROM (
    SELECT 'TICKET' cat, 'VAT-TICKET-8' rule_code, 'VAT 8% - vé xem phim' tax_name, 8 rate,
           SYSTIMESTAMP - INTERVAL '365' DAY(3) vf, CAST(NULL AS TIMESTAMP WITH TIME ZONE) vt, 'APPROVED' st FROM dual UNION ALL
    SELECT 'FNB', 'VAT-FNB-8', 'VAT 8% - thức ăn đồ uống', 8, SYSTIMESTAMP - INTERVAL '365' DAY(3), NULL, 'APPROVED' FROM dual UNION ALL
    SELECT 'FNB', 'VAT-FNB-10-OLD', 'VAT 10% - thức ăn đồ uống (cũ)', 10, SYSTIMESTAMP - INTERVAL '900' DAY(3),
           SYSTIMESTAMP - INTERVAL '365' DAY(3), 'RETIRED' FROM dual UNION ALL
    SELECT 'TICKET', 'VAT-TICKET-5-DRAFT', 'VAT 5% - đề xuất thử nghiệm', 5, SYSTIMESTAMP + INTERVAL '30' DAY, NULL, 'DRAFT' FROM dual
) r
JOIN tax_category c ON c.category_code = r.cat;

UPDATE fnb_product SET tax_category_id = (SELECT tax_category_id FROM tax_category WHERE category_code = 'FNB');

-- Bieu thue thu nhap ca nhan luy tien (mo phong, dung cho bang luong mau)
INSERT INTO pit_rule_set (rule_set_code, rule_set_name, valid_from, source_reference, status, approved_by, approved_at)
SELECT 'PIT-MONTHLY-SIM', 'Biểu thuế lũy tiến theo tháng (mô phỏng)', DATE '2026-01-01',
       'Gia lap phuc vu demo, khong phai tu van thue', 'APPROVED', u.app_user_id, SYSTIMESTAMP - INTERVAL '300' DAY(3)
FROM employee e JOIN app_user u ON u.app_user_id = e.app_user_id
WHERE e.employee_code = 'NV0003';

INSERT INTO pit_rule_bracket (pit_rule_set_id, bracket_no, income_from, income_to, rate_percent, deduction_amount)
SELECT s.pit_rule_set_id, b.n, b.f, b.t, b.r, b.d
FROM pit_rule_set s
CROSS JOIN (
    SELECT 1 n, 0 f, 5000000 t, 5 r, 0 d FROM dual UNION ALL
    SELECT 2, 5000000, 10000000, 10, 250000 FROM dual UNION ALL
    SELECT 3, 10000000, 18000000, 15, 750000 FROM dual UNION ALL
    SELECT 4, 18000000, 32000000, 20, 1650000 FROM dual UNION ALL
    SELECT 5, 32000000, 52000000, 25, 3250000 FROM dual UNION ALL
    SELECT 6, 52000000, 80000000, 30, 5850000 FROM dual UNION ALL
    SELECT 7, 80000000, NULL, 35, 9850000 FROM dual
) b
WHERE s.rule_set_code = 'PIT-MONTHLY-SIM';

-- 17. Suat chieu da chieu xong (6 ngay truoc, 2 suat moi phong moi ngay) va ghe tung suat
DECLARE
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    v_versions t_ids;
    v_n        PLS_INTEGER := 0;
    v_base     DATE := TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE));
    v_idx      PLS_INTEGER := 0;
    v_pick     PLS_INTEGER;
    v_start    TIMESTAMP WITH TIME ZONE;
    v_sid      NUMBER;
    v_film     NUMBER;
    v_runtime  NUMBER;
    v_format   VARCHAR2(40);
BEGIN
    FOR v IN (
        SELECT fv.film_version_id
        FROM film_version fv
        JOIN film f ON f.film_id = fv.film_id
        WHERE f.film_code IN ('TF001', 'TF002', 'TF003', 'TF004', 'TF005', 'TF006', 'TF007', 'TF008', 'TF009', 'TF010')
        ORDER BY fv.film_version_id
    ) LOOP
        v_n := v_n + 1;
        v_versions(v_n) := v.film_version_id;
    END LOOP;

    FOR r IN (
        SELECT cr.room_id, cr.cinema_id, l.layout_id, rt.type_code, rt.type_name
        FROM cinema_room cr
        JOIN cinema c ON c.cinema_id = cr.cinema_id AND c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC')
        JOIN room_type rt ON rt.room_type_id = cr.room_type_id
        JOIN room_layout l ON l.room_id = cr.room_id AND l.status = 'ACTIVE'
        ORDER BY cr.room_id
    ) LOOP
        v_idx := v_idx + 1;
        FOR d IN 1 .. 6 LOOP
            FOR s IN 1 .. 2 LOOP
                v_start := FROM_TZ(CAST(v_base - d + CASE s WHEN 1 THEN 14 ELSE 19 END / 24 AS TIMESTAMP), 'Asia/Ho_Chi_Minh');
                v_pick := MOD(v_idx * 3 + d * 5 + s, v_n) + 1;
                SELECT film_id, runtime_minutes, format_code
                INTO v_film, v_runtime, v_format
                FROM film_version WHERE film_version_id = v_versions(v_pick);

                INSERT INTO screening (cinema_id, room_id, layout_id, film_id, film_version_id,
                                       room_type_code_snapshot, room_type_name_snapshot, format_code,
                                       screening_type_code, starts_at, ends_at, cleanup_minutes, status,
                                       created_at, updated_at)
                VALUES (r.cinema_id, r.room_id, r.layout_id, v_film, v_versions(v_pick),
                        r.type_code, r.type_name, v_format, 'STANDARD', v_start,
                        v_start + NUMTODSINTERVAL(v_runtime, 'MINUTE'), 15, 'COMPLETED',
                        v_start - INTERVAL '10' DAY, v_start + NUMTODSINTERVAL(v_runtime, 'MINUTE'))
                RETURNING screening_id INTO v_sid;

                INSERT INTO show_seat (screening_id, room_seat_id, seat_status, seat_code_snapshot,
                                       row_label_snapshot, seat_label_snapshot, seat_type_code_snapshot)
                SELECT v_sid, rs.room_seat_id, CASE rs.sellable WHEN 'Y' THEN 'AVAILABLE' ELSE 'BLOCKED' END,
                       rs.seat_code, rs.row_label, rs.seat_label, st.type_code
                FROM room_seat rs
                JOIN seat_type st ON st.seat_type_id = rs.seat_type_id
                WHERE rs.layout_id = r.layout_id AND rs.status = 'ACTIVE';
            END LOOP;
        END LOOP;
    END LOOP;
END;
/

-- 18. Don hang, thanh toan, ve, hoan tien, hoa don mo phong, diem thuong, khuyen mai da dung
-- Cong thuc theo BookingServiceImpl: gia ve chua VAT, giam gia chia theo ty le tren tung dong,
-- VAT 8% cong them tren (thanh tien - giam gia), tong = tam tinh - giam gia + VAT.
-- Du lieu sinh bang bo dem gia ngau nhien co dinh nen cac lan chay cho ket qua giong nhau.
DECLARE
    TYPE t_num IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    TYPE t_vc IS TABLE OF VARCHAR2(400) INDEX BY PLS_INTEGER;
    TYPE t_map IS TABLE OF NUMBER INDEX BY VARCHAR2(80);
    TYPE t_line IS RECORD (
        id NUMBER, is_ticket BOOLEAN, seat_id NUMBER, seat_label VARCHAR2(30), name VARCHAR2(300),
        code VARCHAR2(80), qty PLS_INTEGER, unit NUMBER, disc NUMBER, tax NUMBER, total NUMBER,
        taxcat VARCHAR2(20), rule VARCHAR2(40), refunded BOOLEAN, ticket_id NUMBER);
    TYPE t_lines IS TABLE OF t_line INDEX BY PLS_INTEGER;

    v_seed   NUMBER := 20261109;
    v_seq    PLS_INTEGER := 0;
    v_inv    PLS_INTEGER := 0;
    v_now    TIMESTAMP WITH TIME ZONE := SYSTIMESTAMP;

    -- khach hang
    c_user   t_num; c_member t_num; c_year t_num;
    c_name   t_vc;  c_email  t_vc;  c_phone t_vc;
    v_cust   PLS_INTEGER := 0;
    -- khach vang lai
    g_name   t_vc;  g_email  t_vc;  g_phone t_vc;
    -- nhan vien theo rap
    m_mgr    t_num; m_cash t_num;
    -- san pham F&B va gia
    f_id     t_num; f_code t_vc; f_name t_vc;
    f_cnt    PLS_INTEGER := 0;
    f_price  t_map;
    p_cache  t_map;
    -- khuyen mai
    v_welcome NUMBER; v_student NUMBER;
    w_vouch  t_num; s_vouch t_num;
    w_next   PLS_INTEGER := 1; s_next PLS_INTEGER := 1;
    w_used   t_map; s_used t_map;
    v_welcome_spent NUMBER := 0; v_student_spent NUMBER := 0;

    FUNCTION rnd(p_n PLS_INTEGER) RETURN PLS_INTEGER IS
    BEGIN
        v_seed := MOD(v_seed * 1103515245 + 12345, 2147483648);
        RETURN MOD(TRUNC(v_seed / 65536), p_n);
    END;

    FUNCTION hx(p_key VARCHAR2) RETURN VARCHAR2 IS
        r VARCHAR2(64);
    BEGIN
        SELECT UPPER(RAWTOHEX(STANDARD_HASH(p_key, 'SHA256'))) INTO r FROM dual;
        RETURN r;
    END;

    FUNCTION alloc(p_amount NUMBER, p_base NUMBER, p_disc NUMBER) RETURN NUMBER IS
    BEGIN
        IF p_disc = 0 OR p_amount = 0 THEN RETURN 0; END IF;
        IF p_amount >= p_base THEN RETURN p_disc; END IF;
        RETURN LEAST(ROUND(p_disc * p_amount / p_base, 2), p_disc);
    END;

    FUNCTION ticket_price(p_cinema NUMBER, p_rt VARCHAR2, p_seat VARCHAR2, p_stype VARCHAR2,
                          p_rule OUT VARCHAR2) RETURN NUMBER IS
        v_key VARCHAR2(80) := p_cinema || '|' || p_rt || '|' || p_seat || '|' || p_stype;
        v_price NUMBER;
    BEGIN
        IF p_cache.EXISTS(v_key) THEN
            SELECT rule_code INTO p_rule FROM price_rule WHERE price_rule_id = p_cache(v_key);
            SELECT base_price INTO v_price FROM price_rule WHERE price_rule_id = p_cache(v_key);
            RETURN v_price;
        END IF;
        SELECT pr.price_rule_id, pr.rule_code, pr.base_price INTO p_cache(v_key), p_rule, v_price
        FROM price_rule pr
        LEFT JOIN room_type rt ON rt.room_type_id = pr.room_type_id
        LEFT JOIN seat_type st ON st.seat_type_id = pr.seat_type_id
        WHERE pr.approval_status = 'APPROVED'
          AND (pr.cinema_id IS NULL OR pr.cinema_id = p_cinema)
          AND (rt.type_code IS NULL OR rt.type_code = p_rt)
          AND (st.type_code IS NULL OR st.type_code = p_seat)
          AND (pr.screening_type_code IS NULL OR pr.screening_type_code = p_stype)
        ORDER BY pr.priority_no, pr.price_rule_id
        FETCH FIRST 1 ROW ONLY;
        RETURN v_price;
    END;

    PROCEDURE place_order(
        p_scr NUMBER, p_cinema NUMBER, p_le NUMBER, p_cinema_code VARCHAR2, p_cinema_name VARCHAR2,
        p_room_name VARCHAR2, p_film VARCHAR2, p_rt_code VARCHAR2, p_rt_name VARCHAR2, p_stype VARCHAR2,
        p_starts TIMESTAMP WITH TIME ZONE, p_created TIMESTAMP WITH TIME ZONE, p_kind_in VARCHAR2,
        p_nseat_in PLS_INTEGER) IS
        v_kind   VARCHAR2(20) := p_kind_in;
        v_nseat  PLS_INTEGER := p_nseat_in;
        v_future BOOLEAN := p_starts IS NOT NULL AND p_starts > v_now;
        v_paid   BOOLEAN;
        v_pd     VARCHAR2(1);
        v_desc   VARCHAR2(500);
        v_icode  VARCHAR2(80);
        v_channel VARCHAR2(10);
        v_ci     PLS_INTEGER := 0;
        v_user   NUMBER; v_member NUMBER;
        v_gname  VARCHAR2(200); v_gemail VARCHAR2(320); v_gphone VARCHAR2(30);
        v_creator NUMBER;
        v_ids    t_num; v_lbl t_vc; v_stc t_vc;
        v_avail  PLS_INTEGER;
        v_start  PLS_INTEGER;
        L        t_lines;
        m        PLS_INTEGER := 0;
        v_sub    NUMBER := 0; v_disc NUMBER := 0; v_tax NUMBER := 0; v_total NUMBER;
        v_rem_d  NUMBER; v_rem_b NUMBER;
        v_promo  VARCHAR2(20); v_vouch NUMBER; v_pct NUMBER; v_cap NUMBER;
        v_amt    NUMBER; v_ld NUMBER; v_rule VARCHAR2(40); v_price NUMBER;
        v_order  NUMBER; v_ordno VARCHAR2(40);
        v_ostat  VARCHAR2(24);
        v_paid_at TIMESTAMP WITH TIME ZONE; v_expires TIMESTAMP WITH TIME ZONE;
        v_ptxn   NUMBER; v_provider VARCHAR2(40); v_method VARCHAR2(30); v_pref VARCHAR2(150);
        v_type   VARCHAR2(20);
        v_fn     PLS_INTEGER; v_pi PLS_INTEGER; v_qty PLS_INTEGER;
        v_tnum   VARCHAR2(50); v_tid NUMBER; v_tstat VARCHAR2(20);
        v_scan_at TIMESTAMP WITH TIME ZONE; v_scanner NUMBER;
        v_rf     NUMBER; v_rf_amt NUMBER := 0; v_rf_status VARCHAR2(20); v_rf_at TIMESTAMP WITH TIME ZONE;
        v_rf_by  NUMBER; v_rf_reason VARCHAR2(40);
        v_points NUMBER; v_ledger_at TIMESTAMP WITH TIME ZONE;
        v_li     NUMBER;
        v_inv_id NUMBER; v_inv_status VARCHAR2(20); v_att_status VARCHAR2(20);
        v_buyer  VARCHAR2(200); v_buyer_tax VARCHAR2(30); v_buyer_mail VARCHAR2(320); v_buyer_addr VARCHAR2(1000);
        v_seller VARCHAR2(300); v_seller_tax VARCHAR2(30); v_seller_addr VARCHAR2(1000);
        v_issued TIMESTAMP WITH TIME ZONE;
        v_taxable NUMBER;
        v_gate   PLS_INTEGER;
        v_h      VARCHAR2(64);
        v_rr     PLS_INTEGER;
    BEGIN
        v_paid := v_kind IN ('ISSUED', 'REFUNDED', 'PARTIAL', 'REFUND_PENDING');
        v_pd := CASE WHEN v_paid THEN 'Y' ELSE 'N' END;
        IF v_kind = 'PARTIAL' AND v_nseat < 2 THEN v_nseat := 2; END IF;

        -- Kenh ban va nguoi mua
        v_ci := rnd(100);
        v_channel := CASE WHEN p_scr IS NULL THEN CASE WHEN v_ci < 80 THEN 'BOX' ELSE 'KIOSK' END
                          WHEN v_ci < 60 THEN 'ONLINE' WHEN v_ci < 90 THEN 'BOX' ELSE 'KIOSK' END;
        v_ci := 0;
        IF (v_channel = 'ONLINE' AND rnd(100) < 78) OR (v_channel <> 'ONLINE' AND rnd(100) < 18) THEN
            v_ci := rnd(v_cust) + 1;
            v_user := c_user(v_ci);
            IF c_member.EXISTS(v_ci) THEN v_member := c_member(v_ci); END IF;
        ELSE
            v_ci := rnd(8) + 1;
            v_gname := g_name(v_ci); v_gemail := g_email(v_ci); v_gphone := g_phone(v_ci);
            v_ci := 0;
        END IF;
        v_creator := CASE WHEN v_channel = 'ONLINE' THEN NULL ELSE m_cash(p_cinema) END;

        -- Ghe va gia ve
        IF p_scr IS NOT NULL THEN
            SELECT show_seat_id, seat_label_snapshot, seat_type_code_snapshot
            BULK COLLECT INTO v_ids, v_lbl, v_stc
            FROM show_seat
            WHERE screening_id = p_scr AND seat_status = 'AVAILABLE'
            ORDER BY row_label_snapshot, show_seat_id;
            v_avail := v_ids.COUNT;
            IF v_avail < v_nseat THEN v_nseat := v_avail; END IF;
            IF v_nseat <= 0 THEN RETURN; END IF;
            v_start := rnd(v_avail - v_nseat + 1) + 1;
            FOR i IN 1 .. v_nseat LOOP
                m := m + 1;
                L(m).is_ticket := TRUE; L(m).seat_id := v_ids(v_start + i - 1);
                L(m).seat_label := v_lbl(v_start + i - 1); L(m).qty := 1;
                L(m).name := p_film; L(m).taxcat := 'TICKET'; L(m).rule := 'VAT-TICKET-8';
                L(m).unit := ticket_price(p_cinema, p_rt_code, v_stc(v_start + i - 1), p_stype, v_rule);
                L(m).code := v_rule; L(m).refunded := FALSE;
            END LOOP;
        END IF;

        -- F&B: don chi co F&B luon co hang, don ve co 45 phan tram them F&B
        IF p_scr IS NULL OR rnd(100) < 45 THEN
            v_fn := rnd(2) + 1 + CASE WHEN p_scr IS NULL THEN 1 ELSE 0 END;
            FOR i IN 1 .. v_fn LOOP
                v_pi := rnd(f_cnt) + 1; v_qty := rnd(2) + 1;
                IF f_price.EXISTS(p_cinema || '|' || f_id(v_pi)) THEN
                    m := m + 1;
                    L(m).is_ticket := FALSE; L(m).qty := v_qty; L(m).name := f_name(v_pi); L(m).code := f_code(v_pi);
                    L(m).unit := f_price(p_cinema || '|' || f_id(v_pi)); L(m).seat_id := f_id(v_pi);
                    L(m).taxcat := 'FNB'; L(m).rule := 'VAT-FNB-8'; L(m).refunded := FALSE;
                END IF;
            END LOOP;
        END IF;
        IF m = 0 THEN RETURN; END IF;
        IF v_kind = 'PARTIAL' AND m < 2 THEN v_kind := 'REFUNDED'; END IF;

        FOR i IN 1 .. m LOOP
            v_sub := v_sub + L(i).unit * L(i).qty;
        END LOOP;

        -- Khuyen mai (chi don da thanh toan cua hoi vien dang nhap)
        IF v_paid AND v_user IS NOT NULL AND rnd(100) < 22 THEN
            IF c_year(v_ci) >= 2000 AND s_next <= s_vouch.COUNT
               AND (NOT s_used.EXISTS(TO_CHAR(v_user)) OR s_used(TO_CHAR(v_user)) < 2) THEN
                v_promo := 'STUDENT15'; v_vouch := s_vouch(s_next); v_pct := 15; v_cap := 40000;
            ELSIF w_next <= w_vouch.COUNT AND NOT w_used.EXISTS(TO_CHAR(v_user)) THEN
                v_promo := 'WELCOME10'; v_vouch := w_vouch(w_next); v_pct := 10; v_cap := 50000;
            END IF;
        END IF;
        IF v_promo IS NOT NULL THEN
            v_disc := LEAST(ROUND(v_sub * v_pct / 100, 2), v_cap, v_sub);
        END IF;

        v_rem_d := v_disc; v_rem_b := v_sub;
        FOR i IN 1 .. m LOOP
            v_amt := L(i).unit * L(i).qty;
            v_ld := alloc(v_amt, v_rem_b, v_rem_d);
            v_rem_d := v_rem_d - v_ld; v_rem_b := v_rem_b - v_amt;
            L(i).disc := v_ld;
            L(i).tax := ROUND((v_amt - v_ld) * 0.08, 2);
            L(i).total := v_amt - v_ld + L(i).tax;
            v_tax := v_tax + L(i).tax;
        END LOOP;
        v_total := v_sub - v_disc + v_tax;

        v_type := CASE WHEN p_scr IS NULL THEN 'FNB' WHEN L.COUNT > v_nseat THEN 'COMBINED' ELSE 'TICKET' END;
        v_seq := v_seq + 1;
        v_ordno := 'TF' || TO_CHAR(p_created AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || SUBSTR(hx('ORD' || v_seq), 1, 8);
        v_expires := p_created + INTERVAL '10' MINUTE;
        v_paid_at := p_created + NUMTODSINTERVAL(60 + rnd(420), 'SECOND');
        v_ostat := CASE v_kind WHEN 'ISSUED' THEN 'ISSUED' WHEN 'EXPIRED' THEN 'EXPIRED' WHEN 'CANCELLED' THEN 'CANCELLED'
                               WHEN 'REFUNDED' THEN 'REFUNDED' WHEN 'PARTIAL' THEN 'PARTIALLY_REFUNDED'
                               ELSE 'REFUND_PENDING' END;

        INSERT INTO sales_order (order_number, idempotency_key, order_type, sales_channel, cinema_id, legal_entity_id,
                                 app_user_id, member_account_id, guest_name, guest_email, guest_phone,
                                 subtotal_amount, discount_amount, tax_amount, total_amount, order_status,
                                 expires_at, paid_at, issued_at, created_by, created_at, updated_at)
        VALUES (v_ordno, 'seed-order-' || v_seq, v_type, v_channel, p_cinema, p_le,
                v_user, v_member, v_gname, v_gemail, v_gphone,
                v_sub, v_disc, v_tax, v_total, v_ostat,
                v_expires, CASE WHEN v_pd = 'Y' THEN v_paid_at END,
                CASE WHEN v_pd = 'Y' THEN v_paid_at + INTERVAL '5' SECOND END, v_creator, p_created,
                CASE WHEN v_pd = 'Y' THEN v_paid_at + INTERVAL '5' SECOND WHEN v_kind = 'EXPIRED' THEN v_expires
                     ELSE p_created + INTERVAL '3' MINUTE END)
        RETURNING sales_order_id INTO v_order;

        -- Xac dinh dong hoan tien
        IF v_kind = 'REFUNDED' THEN
            FOR i IN 1 .. m LOOP L(i).refunded := TRUE; END LOOP;
        ELSIF v_kind = 'PARTIAL' THEN
            L(1).refunded := TRUE;
        END IF;

        FOR i IN 1 .. m LOOP
            IF L(i).is_ticket THEN
                INSERT INTO sales_order_line (sales_order_id, line_no, item_type, screening_id, show_seat_id,
                                              product_name_snapshot, screening_type_snapshot, room_type_snapshot,
                                              seat_type_snapshot, quantity, unit_price, discount_amount,
                                              tax_category_code, tax_rate_snapshot, tax_amount, line_total,
                                              price_rule_code_snapshot, promotion_snapshot, created_at)
                VALUES (v_order, i, 'TICKET', p_scr, L(i).seat_id, L(i).name, p_stype, p_rt_name,
                        (SELECT seat_type_code_snapshot FROM show_seat WHERE show_seat_id = L(i).seat_id),
                        1, L(i).unit, L(i).disc, 'TICKET', 0.08, L(i).tax, L(i).total, L(i).code,
                        CASE WHEN v_promo IS NOT NULL THEN v_promo END, p_created)
                RETURNING order_line_id INTO L(i).id;

                INSERT INTO seat_hold (show_seat_id, sales_order_id, held_at, expires_at, released_at, hold_status)
                VALUES (L(i).seat_id, v_order, p_created, v_expires,
                        CASE WHEN v_kind = 'EXPIRED' THEN v_expires WHEN v_kind = 'CANCELLED' THEN p_created + INTERVAL '3' MINUTE END,
                        CASE WHEN v_pd = 'Y' THEN 'CONFIRMED' WHEN v_kind = 'EXPIRED' THEN 'EXPIRED' ELSE 'RELEASED' END);

                IF v_paid AND NOT L(i).refunded THEN
                    UPDATE show_seat SET seat_status = 'SOLD', current_order_id = v_order, hold_expires_at = NULL
                    WHERE show_seat_id = L(i).seat_id;
                END IF;
            ELSE
                INSERT INTO sales_order_line (sales_order_id, line_no, item_type, fnb_product_id,
                                              product_name_snapshot, product_code_snapshot, quantity, unit_price,
                                              discount_amount, tax_category_code, tax_rate_snapshot, tax_amount,
                                              line_total, promotion_snapshot, created_at)
                VALUES (v_order, i, 'FNB', L(i).seat_id, L(i).name, L(i).code, L(i).qty, L(i).unit, L(i).disc,
                        'FNB', 0.08, L(i).tax, L(i).total, CASE WHEN v_promo IS NOT NULL THEN v_promo END, p_created)
                RETURNING order_line_id INTO L(i).id;
            END IF;
        END LOOP;

        -- Thanh toan
        IF p_scr IS NULL OR v_channel <> 'ONLINE' THEN
            v_provider := 'POS'; v_method := CASE WHEN rnd(100) < 55 THEN 'CASH' ELSE 'CARD' END;
        ELSE
            v_provider := 'MOCK';
            v_method := CASE rnd(4) WHEN 0 THEN 'MOMO' WHEN 1 THEN 'VNPAY' WHEN 2 THEN 'ZALOPAY' ELSE 'CARD' END;
        END IF;
        IF v_paid THEN
            IF v_channel = 'ONLINE' AND rnd(100) < 8 THEN
                v_h := hx('PF' || v_seq);
                INSERT INTO payment_txn (sales_order_id, provider_code, provider_txn_ref, idempotency_key, payment_method,
                                         amount, payment_status, initiated_at, completed_at, failure_code, failure_message)
                VALUES (v_order, v_provider, v_provider || '-' || SUBSTR(v_h, 1, 24), 'seed-payfail-' || v_seq,
                        v_method, v_total, 'FAILED', p_created + INTERVAL '20' SECOND, p_created + INTERVAL '40' SECOND,
                        'CARD_DECLINED', 'Ngân hàng từ chối giao dịch, khách thử lại bằng phương thức khác.');
            END IF;
            v_pref := v_provider || '-' || SUBSTR(hx('PAY' || v_seq), 1, 24);
            INSERT INTO payment_txn (sales_order_id, provider_code, provider_txn_ref, idempotency_key, payment_method,
                                     amount, payment_status, initiated_at, completed_at)
            VALUES (v_order, v_provider, v_pref, 'seed-pay-' || v_seq, v_method, v_total, 'SUCCEEDED',
                    v_paid_at - INTERVAL '30' SECOND, v_paid_at)
            RETURNING payment_txn_id INTO v_ptxn;
            IF v_provider = 'MOCK' THEN
                v_h := hx('EVT' || v_seq);
                INSERT INTO payment_callback_event (provider_code, provider_event_ref, payment_txn_id, received_at,
                                                    signature_valid, processing_status, payload, processed_at)
                VALUES (v_provider, 'evt-' || SUBSTR(v_h, 1, 24), v_ptxn, v_paid_at, 'Y', 'PROCESSED',
                        '{"orderNumber":"' || v_ordno || '","status":"SUCCEEDED","amount":' || TRUNC(v_total) || '}',
                        v_paid_at + INTERVAL '1' SECOND);
            END IF;
        ELSIF v_kind = 'EXPIRED' AND v_channel = 'ONLINE' AND rnd(100) < 50 THEN
            v_h := hx('PT' || v_seq);
            INSERT INTO payment_txn (sales_order_id, provider_code, provider_txn_ref, idempotency_key, payment_method,
                                     amount, payment_status, initiated_at, completed_at, failure_code, failure_message)
            VALUES (v_order, v_provider, v_provider || '-' || SUBSTR(v_h, 1, 24), 'seed-paytimeout-' || v_seq,
                    v_method, v_total, 'TIMEOUT', p_created + INTERVAL '45' SECOND, v_expires,
                    'PAYMENT_TIMEOUT', 'Không nhận được kết quả thanh toán trước khi đơn hết hạn.');
        ELSIF v_kind = 'CANCELLED' AND v_channel = 'ONLINE' AND rnd(100) < 50 THEN
            v_h := hx('PC' || v_seq);
            INSERT INTO payment_txn (sales_order_id, provider_code, provider_txn_ref, idempotency_key, payment_method,
                                     amount, payment_status, initiated_at, completed_at)
            VALUES (v_order, v_provider, v_provider || '-' || SUBSTR(v_h, 1, 24), 'seed-paycancel-' || v_seq,
                    v_method, v_total, 'CANCELLED', p_created + INTERVAL '45' SECOND, p_created + INTERVAL '2' MINUTE);
        END IF;

        -- Ve va luot quet
        IF v_paid THEN
            v_scanner := m_cash(p_cinema);
            FOR i IN 1 .. m LOOP
                CONTINUE WHEN NOT L(i).is_ticket;
                v_tstat := CASE WHEN L(i).refunded THEN 'REFUNDED'
                                WHEN v_future OR v_kind = 'REFUND_PENDING' THEN 'VALID'
                                WHEN rnd(100) < 94 THEN 'USED' ELSE 'EXPIRED' END;
                v_scan_at := CASE WHEN v_tstat = 'USED' THEN p_starts - NUMTODSINTERVAL(5 + rnd(20), 'MINUTE') END;
                v_tnum := 'T' || SUBSTR(hx('TKT' || v_seq || '-' || i), 1, 20);
                v_h := hx('QR' || v_seq || '-' || i);
                INSERT INTO ticket (order_line_id, ticket_number, qr_token_hash, film_title_snapshot, cinema_name_snapshot,
                                    room_name_snapshot, screening_starts_at, seat_label_snapshot, ticket_status,
                                    issued_at, scanned_at, scanned_by)
                VALUES (L(i).id, v_tnum, LOWER(v_h), p_film, p_cinema_name, p_room_name,
                        p_starts, L(i).seat_label, v_tstat, v_paid_at + INTERVAL '5' SECOND, v_scan_at,
                        CASE WHEN v_tstat = 'USED' THEN v_scanner END)
                RETURNING ticket_id INTO v_tid;
                IF v_tstat = 'USED' THEN
                    v_gate := rnd(3) + 1;
                    v_h := hx('QR' || v_seq || '-' || i);
                    INSERT INTO ticket_scan_event (ticket_id, cinema_id, scanner_user_id, scanned_qr_hash, scan_result,
                                                   scanned_at, device_reference)
                    VALUES (v_tid, p_cinema, v_scanner, LOWER(v_h), 'ACCEPTED', v_scan_at,
                            'GATE-' || p_cinema_code || '-0' || v_gate);
                    IF rnd(25) = 0 THEN
                        v_h := hx('QR' || v_seq || '-' || i);
                        INSERT INTO ticket_scan_event (ticket_id, cinema_id, scanner_user_id, scanned_qr_hash, scan_result,
                                                       scanned_at, device_reference)
                        VALUES (v_tid, p_cinema, v_scanner, LOWER(v_h), 'ALREADY_USED',
                                v_scan_at + INTERVAL '2' MINUTE, 'GATE-' || p_cinema_code || '-0' || v_gate);
                    END IF;
                END IF;
            END LOOP;
        END IF;

        -- Hoan tien
        IF v_kind IN ('REFUNDED', 'PARTIAL', 'REFUND_PENDING') THEN
            v_rf_amt := 0;
            FOR i IN 1 .. m LOOP
                IF L(i).refunded OR v_kind = 'REFUND_PENDING' AND L(i).is_ticket THEN
                    v_rf_amt := v_rf_amt + L(i).total;
                END IF;
            END LOOP;
            v_rf_reason := CASE rnd(4) WHEN 0 THEN 'CUSTOMER_REQUEST' WHEN 1 THEN 'SCHEDULE_CONFLICT'
                                       WHEN 2 THEN 'DUPLICATE_PAYMENT' ELSE 'TECHNICAL_ISSUE' END;
            v_rf_at := LEAST(v_paid_at + NUMTODSINTERVAL(30 + rnd(300), 'MINUTE'), v_now - INTERVAL '1' MINUTE);
            v_rf_by := NVL(v_user, m_mgr(p_cinema));
            v_rf_status := CASE WHEN v_kind = 'REFUND_PENDING' THEN 'PENDING' ELSE 'SUCCEEDED' END;
            v_h := hx('RF' || v_seq);
            v_rr := rnd(100);
            INSERT INTO refund_request (sales_order_id, payment_txn_id, request_number, requested_amount, reason_code,
                                        reason_detail, request_status, requested_by, approved_by, requested_at, approved_at)
            VALUES (v_order, v_ptxn, 'RF' || TO_CHAR(v_rf_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || SUBSTR(v_h, 1, 6),
                    v_rf_amt, v_rf_reason,
                    CASE v_rf_reason WHEN 'CUSTOMER_REQUEST' THEN 'Khách đổi kế hoạch nên không thể xem phim theo lịch đã đặt.'
                                     WHEN 'SCHEDULE_CONFLICT' THEN 'Khách trùng lịch công tác, xin hoàn vé trước giờ chiếu.'
                                     WHEN 'DUPLICATE_PAYMENT' THEN 'Khách bị trừ tiền hai lần cho cùng một đơn.'
                                     ELSE 'Lỗi kỹ thuật khi nhận vé điện tử, khách yêu cầu hoàn tiền.' END,
                    v_rf_status, v_rf_by, CASE WHEN v_kind <> 'REFUND_PENDING' THEN m_mgr(p_cinema) END, v_rf_at,
                    CASE WHEN v_kind <> 'REFUND_PENDING' THEN LEAST(v_rf_at + NUMTODSINTERVAL(10 + v_rr, 'MINUTE'), v_now) END)
            RETURNING refund_request_id INTO v_rf;
            FOR i IN 1 .. m LOOP
                IF L(i).refunded OR v_kind = 'REFUND_PENDING' AND L(i).is_ticket THEN
                    INSERT INTO refund_request_line (refund_request_id, order_line_id, requested_amount, approved_amount, line_status)
                    VALUES (v_rf, L(i).id, L(i).total, CASE WHEN v_kind <> 'REFUND_PENDING' THEN L(i).total END,
                            CASE WHEN v_kind = 'REFUND_PENDING' THEN 'PENDING' ELSE 'SUCCEEDED' END);
                END IF;
            END LOOP;
            IF v_kind <> 'REFUND_PENDING' THEN
                v_h := hx('RFT' || v_seq);
                INSERT INTO refund_txn (refund_request_id, provider_ref, idempotency_key, amount, refund_status,
                                        created_at, completed_at, provider_message)
                VALUES (v_rf, v_provider || '-RF-' || SUBSTR(v_h, 1, 20), 'seed-refund-' || v_seq, v_rf_amt,
                        'SUCCEEDED', v_rf_at + INTERVAL '30' MINUTE, v_rf_at + INTERVAL '31' MINUTE,
                        'Hoàn tiền thành công về phương thức thanh toán ban đầu.');
            END IF;
        END IF;

        -- Diem thuong (1 diem moi 10.000 dong)
        IF v_paid AND v_member IS NOT NULL THEN
            v_points := TRUNC(v_total / 10000);
            IF v_points > 0 THEN
                INSERT INTO member_points_ledger (member_account_id, sales_order_id, idempotency_key, points_delta,
                                                  entry_type, expires_at, reason, created_at)
                VALUES (v_member, v_order, 'EARN-' || v_ordno, v_points, 'EARN', v_paid_at + INTERVAL '365' DAY(3),
                        'Tích điểm đơn hàng ' || v_ordno, v_paid_at + INTERVAL '5' SECOND);
                IF v_kind IN ('REFUNDED', 'PARTIAL') AND TRUNC(v_rf_amt / 10000) > 0 THEN
                    INSERT INTO member_points_ledger (member_account_id, sales_order_id, idempotency_key, points_delta,
                                                      entry_type, reason, created_at)
                    VALUES (v_member, v_order, 'REFUND-' || v_ordno, -TRUNC(v_rf_amt / 10000), 'ADJUST',
                            'Thu hồi điểm do hoàn tiền đơn ' || v_ordno, v_rf_at + INTERVAL '31' MINUTE);
                END IF;
            END IF;
        END IF;

        -- Voucher da dung
        IF v_promo IS NOT NULL THEN
            INSERT INTO promo_redemption (promo_campaign_id, voucher_id, sales_order_id, app_user_id, discount_amount,
                                          redeemed_at, redemption_status)
            VALUES (CASE v_promo WHEN 'WELCOME10' THEN v_welcome ELSE v_student END, v_vouch, v_order, v_user, v_disc,
                    v_paid_at, CASE WHEN v_kind = 'REFUNDED' THEN 'REVERSED' ELSE 'APPLIED' END);
            UPDATE promo_voucher SET status = 'REDEEMED' WHERE voucher_id = v_vouch;
            IF v_promo = 'WELCOME10' THEN
                w_used(TO_CHAR(v_user)) := 1; w_next := w_next + 1;
                IF v_kind <> 'REFUNDED' THEN v_welcome_spent := v_welcome_spent + v_disc; END IF;
            ELSE
                s_used(TO_CHAR(v_user)) := CASE WHEN s_used.EXISTS(TO_CHAR(v_user)) THEN s_used(TO_CHAR(v_user)) + 1 ELSE 1 END;
                s_next := s_next + 1;
                IF v_kind <> 'REFUNDED' THEN v_student_spent := v_student_spent + v_disc; END IF;
            END IF;
        END IF;

        -- Hoa don dien tu mo phong (khoang 35 phan tram don da thanh toan, hoan tien toan bo thi huy hoa don)
        IF v_paid AND (rnd(100) < 35 OR v_kind = 'REFUNDED') THEN
            v_inv := v_inv + 1;
            SELECT display_name, tax_identifier, registered_address INTO v_seller, v_seller_tax, v_seller_addr
            FROM legal_entity_profile WHERE legal_entity_id = p_le AND valid_to IS NULL;
            IF rnd(10) = 0 THEN
                v_buyer := 'Công ty TNHH Thương mại Sao Mai'; v_buyer_tax := '0314' || LPAD(500000 + v_inv, 6, '0');
                v_buyer_mail := 'ketoan@saomai.example'; v_buyer_addr := '15 Lê Duẩn, Quận 1, TP. Hồ Chí Minh';
            ELSIF v_user IS NOT NULL THEN
                v_buyer := c_name(v_ci); v_buyer_tax := NULL; v_buyer_mail := c_email(v_ci); v_buyer_addr := NULL;
            ELSE
                v_buyer := v_gname; v_buyer_tax := NULL; v_buyer_mail := v_gemail; v_buyer_addr := NULL;
            END IF;
            v_att_status := CASE WHEN rnd(100) < 82 THEN 'ACCEPTED' WHEN rnd(100) < 55 THEN 'PENDING'
                                 WHEN rnd(100) < 60 THEN 'TIMEOUT' ELSE 'REJECTED' END;
            IF v_kind = 'REFUNDED' THEN v_att_status := 'ACCEPTED'; END IF;
            v_inv_status := CASE WHEN v_kind = 'REFUNDED' THEN 'CANCELLED' ELSE v_att_status END;
            v_issued := CASE WHEN v_att_status = 'ACCEPTED' THEN v_paid_at + INTERVAL '2' MINUTE END;
            INSERT INTO tax_invoice (sales_order_id, legal_entity_id, invoice_code, idempotency_key, invoice_status,
                                     seller_name_snapshot, seller_tax_id_snapshot, seller_address_snapshot,
                                     buyer_name_snapshot, buyer_tax_id_snapshot, buyer_email_snapshot, buyer_address_snapshot,
                                     subtotal_amount, discount_amount, tax_amount, total_amount, issued_at, created_at)
            VALUES (v_order, p_le, 'MOCK-' || TO_CHAR(v_paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYMMDD') || '-' || LPAD(v_inv, 5, '0'),
                    'seed-invoice-' || v_inv, v_inv_status, v_seller, v_seller_tax, v_seller_addr,
                    v_buyer, v_buyer_tax, v_buyer_mail, v_buyer_addr, v_sub, v_disc, v_tax, v_total, v_issued,
                    v_paid_at + INTERVAL '1' MINUTE)
            RETURNING tax_invoice_id INTO v_inv_id;
            FOR i IN 1 .. m LOOP
                v_taxable := L(i).unit * L(i).qty - L(i).disc;
                IF L(i).is_ticket THEN
                    v_icode := NULL; v_desc := 'Vé xem phim ' || L(i).name || ' - ghế ' || L(i).seat_label;
                ELSE
                    v_icode := L(i).code; v_desc := L(i).name;
                END IF;
                INSERT INTO tax_invoice_line (tax_invoice_id, order_line_id, line_no, item_code_snapshot, description_snapshot,
                                              tax_category_code_snapshot, tax_rule_code_snapshot, tax_rate_snapshot,
                                              quantity, unit_price, discount_amount, taxable_amount, tax_amount, line_total)
                VALUES (v_inv_id, L(i).id, i, v_icode, v_desc,
                        L(i).taxcat, L(i).rule, 8, L(i).qty, L(i).unit, L(i).disc, v_taxable, L(i).tax, L(i).total);
            END LOOP;
            v_h := hx('INV' || v_inv);
            INSERT INTO tax_provider_attempt (tax_invoice_id, provider_code, idempotency_key, attempt_no, attempt_status,
                                              provider_invoice_ref, request_payload, response_payload, error_message,
                                              requested_at, completed_at)
            VALUES (v_inv_id, 'MOCK', 'seed-attempt-' || v_inv, 1, v_att_status,
                    CASE WHEN v_att_status = 'ACCEPTED' THEN 'MOCKREF-' || SUBSTR(v_h, 1, 16) END,
                    '{"orderNumber":"' || v_ordno || '","total":' || TRUNC(v_total) || '}',
                    CASE WHEN v_att_status IN ('ACCEPTED', 'REJECTED') THEN '{"result":"' || v_att_status || '"}' END,
                    CASE v_att_status WHEN 'TIMEOUT' THEN 'Nhà cung cấp mô phỏng không phản hồi trong 10 giây.'
                                      WHEN 'REJECTED' THEN 'Thông tin người mua chưa hợp lệ.' END,
                    v_paid_at + INTERVAL '90' SECOND,
                    CASE WHEN v_att_status <> 'PENDING' THEN v_paid_at + INTERVAL '100' SECOND END);
            INSERT INTO tax_invoice_event (tax_invoice_id, from_status, to_status, event_note, occurred_at)
            VALUES (v_inv_id, NULL, 'DRAFT', 'Tạo hóa đơn từ đơn hàng ' || v_ordno, v_paid_at + INTERVAL '1' MINUTE);
            INSERT INTO tax_invoice_event (tax_invoice_id, from_status, to_status, event_note, occurred_at)
            VALUES (v_inv_id, 'DRAFT', 'PENDING', 'Gửi nhà cung cấp mô phỏng', v_paid_at + INTERVAL '90' SECOND);
            IF v_att_status <> 'PENDING' THEN
                INSERT INTO tax_invoice_event (tax_invoice_id, from_status, to_status, event_note, occurred_at)
                VALUES (v_inv_id, 'PENDING', v_att_status, 'Kết quả từ nhà cung cấp mô phỏng', v_paid_at + INTERVAL '100' SECOND);
            END IF;
            IF v_kind = 'REFUNDED' THEN
                INSERT INTO tax_invoice_event (tax_invoice_id, from_status, to_status, event_note, occurred_at)
                VALUES (v_inv_id, 'ACCEPTED', 'CANCELLED', 'Hủy hóa đơn do hoàn tiền toàn bộ đơn hàng', v_rf_at + INTERVAL '35' MINUTE);
            END IF;
        END IF;
    END place_order;

    FUNCTION pick_kind(p_future BOOLEAN) RETURN VARCHAR2 IS
        r PLS_INTEGER := rnd(100);
    BEGIN
        IF p_future THEN
            RETURN CASE WHEN r < 86 THEN 'ISSUED' WHEN r < 91 THEN 'EXPIRED' WHEN r < 94 THEN 'CANCELLED'
                        WHEN r < 97 THEN 'REFUND_PENDING' ELSE 'REFUNDED' END;
        END IF;
        RETURN CASE WHEN r < 84 THEN 'ISSUED' WHEN r < 90 THEN 'EXPIRED' WHEN r < 94 THEN 'CANCELLED'
                    WHEN r < 97 THEN 'REFUNDED' ELSE 'PARTIAL' END;
    END;
BEGIN
    -- Nap du lieu tham chieu
    FOR u IN (
        SELECT au.app_user_id, au.display_name, au.email, au.phone, cp.date_of_birth, ma.member_account_id
        FROM app_user au
        JOIN customer_profile cp ON cp.app_user_id = au.app_user_id
        LEFT JOIN member_account ma ON ma.app_user_id = au.app_user_id
        ORDER BY au.app_user_id
    ) LOOP
        v_cust := v_cust + 1;
        c_user(v_cust) := u.app_user_id; c_name(v_cust) := u.display_name; c_email(v_cust) := u.email;
        c_phone(v_cust) := u.phone; c_year(v_cust) := EXTRACT(YEAR FROM u.date_of_birth);
        IF u.member_account_id IS NOT NULL THEN c_member(v_cust) := u.member_account_id; END IF;
    END LOOP;

    g_name(1) := 'Nguyễn Văn An';      g_email(1) := 'an.nguyen.vn@example.com';   g_phone(1) := '0987001001';
    g_name(2) := 'Trần Thị Bình';      g_email(2) := 'binh.tran.tt@example.com';   g_phone(2) := '0987001002';
    g_name(3) := 'Lê Hoàng Cường';     g_email(3) := 'cuong.le.hc@example.com';    g_phone(3) := '0987001003';
    g_name(4) := 'Phạm Thị Dung';      g_email(4) := 'dung.pham.tt@example.com';   g_phone(4) := '0987001004';
    g_name(5) := 'Hoàng Minh Đức';     g_email(5) := 'duc.hoang.m@example.com';    g_phone(5) := '0987001005';
    g_name(6) := 'Vũ Thị Giang';       g_email(6) := 'giang.vu.tt@example.com';    g_phone(6) := '0987001006';
    g_name(7) := 'Đỗ Quốc Hưng';       g_email(7) := 'hung.do.q@example.com';      g_phone(7) := '0987001007';
    g_name(8) := 'Bùi Thị Kim Oanh';   g_email(8) := 'oanh.bui.tk@example.com';    g_phone(8) := '0987001008';

    FOR e IN (
        SELECT a.cinema_id, e.app_user_id, p.position_code
        FROM employee e
        JOIN employee_assignment a ON a.employee_id = e.employee_id
        JOIN job_position p ON p.job_position_id = a.job_position_id
        WHERE e.app_user_id IS NOT NULL AND p.position_code IN ('CINEMA_MGR', 'CASHIER')
        ORDER BY e.employee_code
    ) LOOP
        IF e.position_code = 'CINEMA_MGR' AND NOT m_mgr.EXISTS(e.cinema_id) THEN m_mgr(e.cinema_id) := e.app_user_id; END IF;
        IF e.position_code = 'CASHIER' AND NOT m_cash.EXISTS(e.cinema_id) THEN m_cash(e.cinema_id) := e.app_user_id; END IF;
    END LOOP;

    FOR p IN (SELECT fnb_product_id, product_code, product_name FROM fnb_product ORDER BY fnb_product_id) LOOP
        f_cnt := f_cnt + 1;
        f_id(f_cnt) := p.fnb_product_id; f_code(f_cnt) := p.product_code; f_name(f_cnt) := p.product_name;
    END LOOP;
    FOR p IN (SELECT fnb_product_id, cinema_id, unit_price FROM fnb_product_price WHERE approval_status = 'APPROVED') LOOP
        f_price(p.cinema_id || '|' || p.fnb_product_id) := p.unit_price;
    END LOOP;

    SELECT promo_campaign_id INTO v_welcome FROM promo_campaign WHERE campaign_code = 'WELCOME10';
    SELECT promo_campaign_id INTO v_student FROM promo_campaign WHERE campaign_code = 'STUDENT15';
    SELECT voucher_id BULK COLLECT INTO w_vouch FROM promo_voucher WHERE promo_campaign_id = v_welcome ORDER BY voucher_code;
    SELECT voucher_id BULK COLLECT INTO s_vouch FROM promo_voucher WHERE promo_campaign_id = v_student ORDER BY voucher_code;

    -- Don ve cho suat da chieu va suat sap chieu trong 36 gio toi
    FOR s IN (
        SELECT sc.screening_id, sc.cinema_id, c.legal_entity_id, c.cinema_code, c.cinema_name, r.room_name,
               f.title_local, sc.room_type_code_snapshot rt_code, sc.room_type_name_snapshot rt_name,
               sc.screening_type_code, sc.starts_at
        FROM screening sc
        JOIN cinema c ON c.cinema_id = sc.cinema_id
        JOIN cinema_room r ON r.room_id = sc.room_id
        JOIN film f ON f.film_id = sc.film_id
        WHERE sc.status = 'COMPLETED' OR (sc.status = 'ON_SALE' AND sc.starts_at < v_now + INTERVAL '36' HOUR)
        ORDER BY sc.screening_id
    ) LOOP
        IF s.starts_at > v_now THEN
            FOR k IN 1 .. 2 + rnd(5) LOOP
                place_order(s.screening_id, s.cinema_id, s.legal_entity_id, s.cinema_code, s.cinema_name, s.room_name,
                            s.title_local, s.rt_code, s.rt_name, s.screening_type_code, s.starts_at,
                            v_now - NUMTODSINTERVAL(5 + rnd(1500), 'MINUTE'), pick_kind(TRUE), 1 + rnd(4));
            END LOOP;
        ELSE
            FOR k IN 1 .. 4 + rnd(9) LOOP
                place_order(s.screening_id, s.cinema_id, s.legal_entity_id, s.cinema_code, s.cinema_name, s.room_name,
                            s.title_local, s.rt_code, s.rt_name, s.screening_type_code, s.starts_at,
                            s.starts_at - NUMTODSINTERVAL(30 + rnd(2600), 'MINUTE'), pick_kind(FALSE), 1 + rnd(4));
            END LOOP;
        END IF;
    END LOOP;

    -- Don chi mua F&B tai quay trong 6 ngay qua
    FOR c IN (SELECT cinema_id, legal_entity_id, cinema_code, cinema_name FROM cinema
              WHERE cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC') ORDER BY cinema_id) LOOP
        FOR d IN 1 .. 6 LOOP
            FOR k IN 1 .. 5 + rnd(6) LOOP
                place_order(NULL, c.cinema_id, c.legal_entity_id, c.cinema_code, c.cinema_name, NULL, NULL, NULL, NULL, NULL,
                            NULL,
                            FROM_TZ(CAST(TRUNC(CAST(v_now AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE)) - d
                                         + (10 * 60 + rnd(12 * 60)) / 1440 AS TIMESTAMP), 'Asia/Ho_Chi_Minh'),
                            CASE WHEN rnd(100) < 96 THEN 'ISSUED' ELSE 'CANCELLED' END, 0);
            END LOOP;
        END LOOP;
    END LOOP;

    UPDATE promo_campaign SET budget_used = v_welcome_spent WHERE promo_campaign_id = v_welcome;
    UPDATE promo_campaign SET budget_used = v_student_spent WHERE promo_campaign_id = v_student;
END;
/

-- 19. Diem chao mung, doi diem va xep hang thanh vien theo tong diem
INSERT INTO member_points_ledger (member_account_id, idempotency_key, points_delta, entry_type, reason, created_at)
SELECT member_account_id, 'WELCOME-' || member_number, 10, 'ADJUST', 'Điểm chào mừng thành viên mới', joined_at
FROM member_account;

INSERT INTO member_points_ledger (member_account_id, idempotency_key, points_delta, entry_type, reason, created_at)
SELECT m.member_account_id, 'REDEEM-' || m.member_number, -50, 'REDEEM', 'Đổi 1 bắp ngọt size vừa miễn phí',
       SYSTIMESTAMP - INTERVAL '2' DAY
FROM member_account m
WHERE (SELECT SUM(points_delta) FROM member_points_ledger l WHERE l.member_account_id = m.member_account_id) >= 150;

UPDATE member_account m
SET member_tier_id = (
    SELECT t.member_tier_id FROM member_tier t
    WHERE t.minimum_points = (
        SELECT MAX(t2.minimum_points) FROM member_tier t2
        WHERE t2.minimum_points <= NVL((SELECT SUM(l.points_delta) FROM member_points_ledger l
                                        WHERE l.member_account_id = m.member_account_id), 0)));

-- 20. Kho va ton kho F&B (nhap kho 30 ngay truoc, chuyen quay 8 ngay truoc, tru kho theo don ban)
INSERT INTO inventory_location (cinema_id, location_code, location_name, location_type)
SELECT c.cinema_id, l.code, l.name, l.typ
FROM cinema c
CROSS JOIN (
    SELECT 'KHO' code, 'Kho nguyên liệu' name, 'WAREHOUSE' typ FROM dual UNION ALL
    SELECT 'QUAY', 'Quầy bán hàng', 'CONCESSION' FROM dual
) l
WHERE c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC');

DECLARE
    TYPE t_map IS TABLE OF NUMBER INDEX BY VARCHAR2(60);
    v_qty    t_map;      -- ton quay hien tai theo rap|san pham
    v_sold   t_map;      -- tong ban theo rap|san pham
    v_cost   t_map;      -- gia von theo san pham
    v_loc    t_map;      -- vi tri theo rap|ma
    v_txn    NUMBER;
    v_seq    PLS_INTEGER := 0;
    v_recv   NUMBER;
    v_trans  NUMBER;
    v_key    VARCHAR2(60);
    v_before NUMBER;
    v_mgr    NUMBER;
    v_now    TIMESTAMP WITH TIME ZONE := SYSTIMESTAMP;

    PROCEDURE sale_line(p_txn NUMBER, p_cinema NUMBER, p_product NUMBER, p_line NUMBER, p_qty NUMBER) IS
        k VARCHAR2(60) := p_cinema || '|' || p_product;
    BEGIN
        v_before := v_qty(k);
        v_qty(k) := v_before - p_qty;
        INSERT INTO inventory_txn_line (inventory_txn_id, fnb_product_id, sales_order_line_id, quantity_delta,
                                        quantity_before, quantity_after, unit_cost)
        VALUES (p_txn, p_product, p_line, -p_qty, v_before, v_qty(k), v_cost(TO_CHAR(p_product)));
    END;
BEGIN
    FOR p IN (SELECT fnb_product_id, product_code FROM fnb_product WHERE product_type = 'ITEM') LOOP
        v_cost(TO_CHAR(p.fnb_product_id)) := CASE
            WHEN p.product_code LIKE 'POPCORN%' THEN 12000 WHEN p.product_code LIKE 'SODA%' THEN 6000
            WHEN p.product_code LIKE 'WATER%' THEN 4000 ELSE 15000 END;
    END LOOP;

    FOR l IN (SELECT inventory_location_id, cinema_id, location_code FROM inventory_location) LOOP
        v_loc(l.cinema_id || '|' || l.location_code) := l.inventory_location_id;
    END LOOP;

    -- Tong luong ban theo rap va thanh phan (combo duoc tach theo cong thuc)
    FOR s IN (
        SELECT cinema_id, pid, SUM(q) q FROM (
            SELECT o.cinema_id, NVL(rc.component_product_id, l.fnb_product_id) pid, l.quantity * NVL(rc.quantity, 1) q
            FROM sales_order o
            JOIN sales_order_line l ON l.sales_order_id = o.sales_order_id AND l.item_type = 'FNB'
            JOIN fnb_product p ON p.fnb_product_id = l.fnb_product_id
            LEFT JOIN fnb_recipe_component rc ON rc.combo_product_id = p.fnb_product_id AND p.product_type = 'COMBO'
            WHERE o.order_status IN ('ISSUED', 'PARTIALLY_REFUNDED', 'REFUND_PENDING')
        ) GROUP BY cinema_id, pid
    ) LOOP
        v_sold(s.cinema_id || '|' || s.pid) := s.q;
    END LOOP;

    FOR c IN (SELECT cinema_id, cinema_code FROM cinema WHERE cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC') ORDER BY cinema_id) LOOP
        SELECT e.app_user_id INTO v_mgr
        FROM employee e JOIN employee_assignment a ON a.employee_id = e.employee_id AND a.cinema_id = c.cinema_id
        JOIN job_position jp ON jp.job_position_id = a.job_position_id AND jp.position_code = 'CINEMA_MGR';

        -- Nhap kho, chuyen kho sang quay: moi phieu gom cac mat hang theo doi ton
        v_seq := v_seq + 1;
        INSERT INTO inventory_txn (inventory_location_id, txn_number, txn_type, reference_type, reference_code, reason,
                                   created_by, approved_by, occurred_at)
        VALUES (v_loc(c.cinema_id || '|KHO'), 'GR' || TO_CHAR(v_seq, 'FM0000'), 'RECEIPT', 'PURCHASE_ORDER',
                'PO-' || c.cinema_code || '-001', 'Nhập hàng định kỳ từ nhà cung cấp', v_mgr, v_mgr, v_now - INTERVAL '30' DAY)
        RETURNING inventory_txn_id INTO v_txn;
        FOR p IN (SELECT fnb_product_id FROM fnb_product WHERE product_type = 'ITEM' ORDER BY fnb_product_id) LOOP
            v_key := c.cinema_id || '|' || p.fnb_product_id;
            v_recv := CEIL((CASE WHEN v_sold.EXISTS(v_key) THEN v_sold(v_key) ELSE 0 END + 140) / 50) * 50;
            INSERT INTO inventory_txn_line (inventory_txn_id, fnb_product_id, quantity_delta, quantity_before, quantity_after, unit_cost)
            VALUES (v_txn, p.fnb_product_id, v_recv, 0, v_recv, v_cost(TO_CHAR(p.fnb_product_id)));
            v_qty(v_key) := v_recv;
        END LOOP;

        v_seq := v_seq + 1;
        INSERT INTO inventory_txn (inventory_location_id, txn_number, txn_type, reference_type, reference_code, reason,
                                   created_by, approved_by, occurred_at)
        VALUES (v_loc(c.cinema_id || '|KHO'), 'TO' || TO_CHAR(v_seq, 'FM0000'), 'TRANSFER_OUT', 'TRANSFER',
                'TF-' || c.cinema_code || '-001', 'Chuyển hàng ra quầy bán', v_mgr, v_mgr, v_now - INTERVAL '8' DAY)
        RETURNING inventory_txn_id INTO v_txn;
        DECLARE
            v_in NUMBER;
        BEGIN
            v_seq := v_seq + 1;
            INSERT INTO inventory_txn (inventory_location_id, txn_number, txn_type, reference_type, reference_code, reason,
                                       created_by, approved_by, occurred_at)
            VALUES (v_loc(c.cinema_id || '|QUAY'), 'TI' || TO_CHAR(v_seq, 'FM0000'), 'TRANSFER_IN', 'TRANSFER',
                    'TF-' || c.cinema_code || '-001', 'Nhận hàng từ kho', v_mgr, v_mgr, v_now - INTERVAL '8' DAY)
            RETURNING inventory_txn_id INTO v_in;
            FOR p IN (SELECT fnb_product_id FROM fnb_product WHERE product_type = 'ITEM' ORDER BY fnb_product_id) LOOP
                v_key := c.cinema_id || '|' || p.fnb_product_id;
                v_recv := v_qty(v_key);
                v_trans := (CASE WHEN v_sold.EXISTS(v_key) THEN v_sold(v_key) ELSE 0 END) + 40;
                INSERT INTO inventory_txn_line (inventory_txn_id, fnb_product_id, quantity_delta, quantity_before, quantity_after, unit_cost)
                VALUES (v_txn, p.fnb_product_id, -v_trans, v_recv, v_recv - v_trans, v_cost(TO_CHAR(p.fnb_product_id)));
                INSERT INTO inventory_txn_line (inventory_txn_id, fnb_product_id, quantity_delta, quantity_before, quantity_after, unit_cost)
                VALUES (v_in, p.fnb_product_id, v_trans, 0, v_trans, v_cost(TO_CHAR(p.fnb_product_id)));
                INSERT INTO inventory_balance (inventory_location_id, fnb_product_id, on_hand_quantity)
                VALUES (v_loc(c.cinema_id || '|KHO'), p.fnb_product_id, v_recv - v_trans);
                v_qty(v_key) := v_trans;
            END LOOP;
        END;
    END LOOP;

    -- Tru kho theo tung don F&B theo thu tu thanh toan
    FOR o IN (
        SELECT o.sales_order_id, o.order_number, o.cinema_id, o.paid_at
        FROM sales_order o
        WHERE o.order_status IN ('ISSUED', 'PARTIALLY_REFUNDED', 'REFUND_PENDING')
          AND EXISTS (SELECT 1 FROM sales_order_line l WHERE l.sales_order_id = o.sales_order_id AND l.item_type = 'FNB')
        ORDER BY o.paid_at, o.sales_order_id
    ) LOOP
        INSERT INTO inventory_txn (inventory_location_id, txn_number, txn_type, reference_type, reference_code, reason,
                                   created_by, occurred_at)
        VALUES (v_loc(o.cinema_id || '|QUAY'), 'SL-' || o.order_number, 'SALE', 'SALES_ORDER', o.order_number,
                'Trừ kho theo đơn bán hàng', NULL, o.paid_at)
        RETURNING inventory_txn_id INTO v_txn;
        FOR l IN (
            SELECT l.order_line_id, l.fnb_product_id, l.quantity, p.product_type
            FROM sales_order_line l JOIN fnb_product p ON p.fnb_product_id = l.fnb_product_id
            WHERE l.sales_order_id = o.sales_order_id AND l.item_type = 'FNB'
            ORDER BY l.line_no
        ) LOOP
            IF l.product_type = 'ITEM' THEN
                sale_line(v_txn, o.cinema_id, l.fnb_product_id, l.order_line_id, l.quantity);
            ELSE
                FOR rc IN (SELECT component_product_id, quantity FROM fnb_recipe_component
                           WHERE combo_product_id = l.fnb_product_id ORDER BY component_product_id) LOOP
                    sale_line(v_txn, o.cinema_id, rc.component_product_id, NULL, l.quantity * rc.quantity);
                END LOOP;
            END IF;
        END LOOP;
    END LOOP;

    -- Huy hang het han tai quay roi chot ton cuoi cua quay
    FOR c IN (SELECT cinema_id, cinema_code FROM cinema WHERE cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC') ORDER BY cinema_id) LOOP
        SELECT e.app_user_id INTO v_mgr
        FROM employee e JOIN employee_assignment a ON a.employee_id = e.employee_id AND a.cinema_id = c.cinema_id
        JOIN job_position jp ON jp.job_position_id = a.job_position_id AND jp.position_code = 'CINEMA_MGR';
        v_seq := v_seq + 1;
        INSERT INTO inventory_txn (inventory_location_id, txn_number, txn_type, reference_type, reference_code, reason,
                                   created_by, approved_by, occurred_at)
        VALUES (v_loc(c.cinema_id || '|QUAY'), 'WS' || TO_CHAR(v_seq, 'FM0000'), 'WASTE', 'STOCK_ADJUSTMENT',
                'WS-' || c.cinema_code || '-001', 'Hủy bắp và nachos quá hạn sử dụng trong ca', v_mgr, v_mgr, v_now - INTERVAL '1' MINUTE)
        RETURNING inventory_txn_id INTO v_txn;
        FOR p IN (SELECT fnb_product_id, product_code FROM fnb_product
                  WHERE product_type = 'ITEM' ORDER BY fnb_product_id) LOOP
            v_key := c.cinema_id || '|' || p.fnb_product_id;
            IF p.product_code LIKE 'POPCORN%' OR p.product_code LIKE 'NACHOS%' THEN
                v_before := v_qty(v_key);
                v_qty(v_key) := v_before - 3;
                INSERT INTO inventory_txn_line (inventory_txn_id, fnb_product_id, quantity_delta, quantity_before, quantity_after, unit_cost)
                VALUES (v_txn, p.fnb_product_id, -3, v_before, v_qty(v_key), v_cost(TO_CHAR(p.fnb_product_id)));
            END IF;
            INSERT INTO inventory_balance (inventory_location_id, fnb_product_id, on_hand_quantity)
            VALUES (v_loc(c.cinema_id || '|QUAY'), p.fnb_product_id, v_qty(v_key));
        END LOOP;
    END LOOP;
END;
/

-- 21. Ca lam viec 7 ngay qua va 3 ngay toi, cham cong, ban giao quy
DECLARE
    v_now   TIMESTAMP WITH TIME ZONE := SYSTIMESTAMP;
    v_today DATE := TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' AS DATE));
    v_idx   PLS_INTEGER := 0;
    v_tpl   NUMBER;
    v_start TIMESTAMP WITH TIME ZONE;
    v_end   TIMESTAMP WITH TIME ZONE;
    v_shift NUMBER;
    v_mgr   NUMBER;
    v_hr    NUMBER;
    v_code  VARCHAR2(30);
    v_status VARCHAR2(20);
    v_late  PLS_INTEGER;
    v_in    TIMESTAMP WITH TIME ZONE;
    v_out   TIMESTAMP WITH TIME ZONE;
    v_cash  NUMBER;
    v_var   NUMBER;
    v_open  CONSTANT NUMBER := 2000000;
BEGIN
    SELECT e.app_user_id INTO v_hr FROM employee e WHERE e.employee_code = 'NV0002';

    FOR e IN (
        SELECT em.employee_id, em.employee_code, em.app_user_id, a.cinema_id, jp.position_code
        FROM employee em
        JOIN employee_assignment a ON a.employee_id = em.employee_id
        JOIN job_position jp ON jp.job_position_id = a.job_position_id
        JOIN department d ON d.department_id = a.department_id
        WHERE d.department_code IN ('OPS', 'FNB')
        ORDER BY em.employee_code
    ) LOOP
        v_idx := v_idx + 1;
        SELECT em.app_user_id INTO v_mgr
        FROM employee em JOIN employee_assignment a ON a.employee_id = em.employee_id AND a.cinema_id = e.cinema_id
        JOIN job_position jp ON jp.job_position_id = a.job_position_id AND jp.position_code = 'CINEMA_MGR';

        v_code := CASE WHEN e.position_code = 'CINEMA_MGR' OR MOD(v_idx, 2) = 0 THEN 'CA_SANG' ELSE 'CA_TOI' END;
        SELECT shift_template_id INTO v_tpl FROM shift_template WHERE cinema_id = e.cinema_id AND shift_code = v_code;

        FOR d IN -7 .. 3 LOOP
            CONTINUE WHEN MOD(v_idx + d + 70, 7) = 0;
            v_start := FROM_TZ(CAST(v_today + d + CASE v_code WHEN 'CA_SANG' THEN 8 ELSE 15 END / 24 AS TIMESTAMP), 'Asia/Ho_Chi_Minh');
            v_end := v_start + INTERVAL '8' HOUR;
            v_status := CASE WHEN v_end < v_now THEN 'COMPLETED' WHEN d <= 1 THEN 'CONFIRMED' ELSE 'PLANNED' END;

            INSERT INTO shift_assignment (employee_id, cinema_id, shift_template_id, starts_at, ends_at, duty_code,
                                          assignment_status, assigned_by, created_at)
            VALUES (e.employee_id, e.cinema_id, v_tpl, v_start, v_end,
                    CASE e.position_code WHEN 'CINEMA_MGR' THEN 'QUAN_LY' WHEN 'CASHIER' THEN 'BAN_VE'
                                         WHEN 'USHER' THEN 'SOAT_VE' ELSE 'QUAY_FNB' END,
                    v_status, v_mgr, LEAST(v_start - INTERVAL '5' DAY, v_now))
            RETURNING shift_assignment_id INTO v_shift;

            IF v_status = 'COMPLETED' THEN
                v_late := MOD(v_idx * 7 + d * 3 + 100, 17) - 6;          -- am: den som, duong: den tre (phut)
                v_in := v_start + NUMTODSINTERVAL(v_late, 'MINUTE');
                v_out := LEAST(v_end + NUMTODSINTERVAL(MOD(v_idx + d + 50, 20), 'MINUTE'), v_now - INTERVAL '1' MINUTE);
                INSERT INTO attendance_record (shift_assignment_id, clock_in_at, clock_out_at, attendance_status,
                                               approved_by, approved_at, note)
                VALUES (v_shift, v_in, v_out, CASE WHEN d >= -1 THEN 'SUBMITTED' ELSE 'APPROVED' END,
                        CASE WHEN d < -1 THEN CASE WHEN e.position_code = 'CINEMA_MGR' THEN v_hr ELSE v_mgr END END,
                        CASE WHEN d < -1 THEN v_end + INTERVAL '1' DAY END,
                        CASE WHEN v_late > 8 THEN 'Đến trễ, đã báo quản lý.' END);

                IF e.position_code = 'CASHIER' AND e.app_user_id IS NOT NULL THEN
                    SELECT NVL(SUM(p.amount), 0) INTO v_cash
                    FROM payment_txn p JOIN sales_order o ON o.sales_order_id = p.sales_order_id
                    WHERE o.cinema_id = e.cinema_id AND p.payment_method = 'CASH' AND p.payment_status = 'SUCCEEDED'
                      AND p.completed_at >= v_start AND p.completed_at < v_end;
                    v_var := CASE MOD(v_idx + d + 100, 6) WHEN 0 THEN -20000 WHEN 1 THEN 10000 ELSE 0 END;
                    INSERT INTO shift_handover (shift_assignment_id, opening_cash, cash_sales, cash_refunds, closing_cash,
                                                variance_amount, handover_note, submitted_by, reviewed_by, handover_status,
                                                submitted_at, reviewed_at)
                    VALUES (v_shift, v_open, v_cash, 0, v_open + v_cash + v_var, v_var,
                            CASE WHEN v_var <> 0 THEN 'Chênh lệch quỹ đã ghi nhận, quản lý rạp xác nhận.' ELSE 'Quỹ khớp với báo cáo bán hàng.' END,
                            e.app_user_id, CASE WHEN d < -1 THEN v_mgr END,
                            CASE WHEN d < -1 THEN 'APPROVED' ELSE 'PENDING' END,
                            v_out + INTERVAL '10' MINUTE, CASE WHEN d < -1 THEN v_end + INTERVAL '1' DAY END);
                END IF;
            ELSIF v_status = 'CONFIRMED' AND d = 0 AND v_start < v_now THEN
                INSERT INTO attendance_record (shift_assignment_id, clock_in_at, attendance_status)
                VALUES (v_shift, v_start + INTERVAL '2' MINUTE, 'OPEN');
            END IF;
        END LOOP;
    END LOOP;
END;
/

-- Dieu chinh cham cong (3 yeu cau) va don nghi phep
INSERT INTO attendance_change (attendance_record_id, field_code, old_value, requested_value, reason, request_status,
                               requested_by, reviewed_by, requested_at, reviewed_at)
SELECT x.attendance_record_id, 'CLOCK_OUT', TO_CHAR(x.clock_out_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'),
       TO_CHAR((x.clock_out_at + INTERVAL '45' MINUTE) AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'),
       'Ở lại hỗ trợ dọn phòng chiếu sau suất cuối nhưng quên quẹt thẻ ra.',
       CASE x.rn WHEN 1 THEN 'APPROVED' WHEN 2 THEN 'REJECTED' ELSE 'PENDING' END,
       x.user_id, CASE WHEN x.rn < 3 THEN x.mgr_id END, x.clock_out_at + INTERVAL '2' HOUR,
       CASE WHEN x.rn < 3 THEN x.clock_out_at + INTERVAL '1' DAY END
FROM (
    SELECT ar.attendance_record_id, ar.clock_out_at, em.app_user_id user_id,
           (SELECT m.app_user_id FROM employee m
              JOIN employee_assignment ma ON ma.employee_id = m.employee_id AND ma.cinema_id = sa.cinema_id
              JOIN job_position mp ON mp.job_position_id = ma.job_position_id AND mp.position_code = 'CINEMA_MGR') mgr_id,
           ROW_NUMBER() OVER (ORDER BY ar.attendance_record_id) rn
    FROM attendance_record ar
    JOIN shift_assignment sa ON sa.shift_assignment_id = ar.shift_assignment_id
    JOIN employee em ON em.employee_id = sa.employee_id
    WHERE ar.attendance_status = 'APPROVED' AND em.app_user_id IS NOT NULL AND sa.duty_code = 'QUAY_FNB'
) x
WHERE x.rn <= 3;

INSERT INTO leave_request (employee_id, leave_type_id, starts_at, ends_at, reason, request_status, requested_by,
                           reviewed_by, requested_at, reviewed_at)
SELECT e.employee_id, lt.leave_type_id,
       FROM_TZ(CAST(TRUNC(SYSDATE) + r.d1 AS TIMESTAMP), 'Asia/Ho_Chi_Minh'),
       FROM_TZ(CAST(TRUNC(SYSDATE) + r.d2 AS TIMESTAMP), 'Asia/Ho_Chi_Minh'),
       r.reason, r.st, e.app_user_id,
       CASE WHEN r.st IN ('APPROVED', 'REJECTED') THEN (SELECT app_user_id FROM employee WHERE employee_code = r.reviewer) END,
       SYSTIMESTAMP - NUMTODSINTERVAL(r.ago, 'DAY'),
       CASE WHEN r.st IN ('APPROVED', 'REJECTED') THEN SYSTIMESTAMP - NUMTODSINTERVAL(r.ago - 1, 'DAY') END
FROM (
    SELECT 'NV0102' ec, 'ANNUAL' lt, 7 d1, 9 d2, 'Nghỉ phép năm về quê thăm gia đình.' reason, 'APPROVED' st, 'NV0101' reviewer, 5 ago FROM dual UNION ALL
    SELECT 'NV0203', 'SICK', -12, -10, 'Sốt siêu vi, có giấy khám bệnh.', 'APPROVED', 'NV0201', 13 FROM dual UNION ALL
    SELECT 'NV0302', 'PERSONAL', 12, 13, 'Việc gia đình (đám cưới em trai).', 'PENDING', 'NV0301', 2 FROM dual UNION ALL
    SELECT 'NV0105', 'ANNUAL', 4, 6, 'Đi du lịch cùng bạn bè.', 'REJECTED', 'NV0101', 8 FROM dual UNION ALL
    SELECT 'NV0402', 'UNPAID', 20, 24, 'Xin nghỉ không lương để lo việc học.', 'APPROVED', 'NV0401', 6 FROM dual UNION ALL
    SELECT 'NV0005', 'ANNUAL', 15, 17, 'Nghỉ phép năm.', 'PENDING', 'NV0004', 1 FROM dual
) r
JOIN employee e ON e.employee_code = r.ec
JOIN leave_type lt ON lt.leave_code = r.lt;

-- 22. Bang luong 2 thang gan nhat (mo phong thue TNCN va bao hiem, dung bieu PIT-MONTHLY-SIM)
-- Bao hiem bat buoc tinh 10,5 phan tram luong co ban, giam tru gia canh ban than 11.000.000 dong/thang.
DECLARE
    v_chain  NUMBER;
    v_set    NUMBER;
    v_acc    NUMBER;
    v_gm     NUMBER;
    v_period NUMBER;
    v_run    NUMBER;
    v_pe     NUMBER;
    v_first  DATE;
    v_last   DATE;
    v_allow  NUMBER; v_bonus NUMBER; v_ins NUMBER; v_gross NUMBER; v_taxable NUMBER; v_pit NUMBER; v_net NUMBER;
    v_rate   NUMBER; v_ded NUMBER;
    c_personal CONSTANT NUMBER := 11000000;
BEGIN
    SELECT chain_id INTO v_chain FROM org_chain WHERE chain_code = 'THEFILM';
    SELECT pit_rule_set_id INTO v_set FROM pit_rule_set WHERE rule_set_code = 'PIT-MONTHLY-SIM';
    SELECT app_user_id INTO v_acc FROM employee WHERE employee_code = 'NV0003';
    SELECT app_user_id INTO v_gm FROM employee WHERE employee_code = 'NV0001';

    FOR k IN 1 .. 2 LOOP
        v_first := ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -k);
        v_last := LAST_DAY(v_first);
        INSERT INTO payroll_period (chain_id, period_code, period_start, period_end, period_status)
        VALUES (v_chain, 'PR-' || TO_CHAR(v_first, 'YYYYMM'), v_first, v_last, 'CLOSED')
        RETURNING payroll_period_id INTO v_period;

        INSERT INTO payroll_run (payroll_period_id, run_number, run_status, created_by, approved_by, created_at, approved_at)
        VALUES (v_period, 1, 'PAID', v_acc, v_gm,
                FROM_TZ(CAST(v_last + 2 AS TIMESTAMP), 'Asia/Ho_Chi_Minh'),
                FROM_TZ(CAST(v_last + 4 AS TIMESTAMP), 'Asia/Ho_Chi_Minh'))
        RETURNING payroll_run_id INTO v_run;

        FOR e IN (
            SELECT em.employee_id, em.employee_code, c.base_salary, jp.position_code
            FROM employee em
            JOIN employee_contract c ON c.employee_id = em.employee_id AND c.contract_status = 'ACTIVE'
            JOIN employee_assignment a ON a.employee_id = em.employee_id
            JOIN job_position jp ON jp.job_position_id = a.job_position_id
            ORDER BY em.employee_code
        ) LOOP
            v_allow := 730000 + CASE e.position_code WHEN 'GM' THEN 10000000 WHEN 'CINEMA_MGR' THEN 3000000
                                                     WHEN 'CS_LEAD' THEN 2000000 WHEN 'HR_MGR' THEN 3000000 ELSE 0 END;
            v_bonus := CASE WHEN e.position_code IN ('CASHIER', 'CONCESSION', 'USHER')
                            THEN MOD(e.employee_id + k, 3) * 250000 ELSE 0 END;
            v_ins := ROUND(e.base_salary * 0.105);
            v_gross := e.base_salary + v_allow + v_bonus;
            v_taxable := GREATEST(0, v_gross - v_ins - c_personal);
            v_pit := 0;
            IF v_taxable > 0 THEN
                SELECT rate_percent, deduction_amount INTO v_rate, v_ded
                FROM pit_rule_bracket
                WHERE pit_rule_set_id = v_set AND income_from < v_taxable AND (income_to IS NULL OR v_taxable <= income_to);
                v_pit := GREATEST(0, ROUND(v_taxable * v_rate / 100 - v_ded));
            END IF;
            v_net := v_gross - v_ins - v_pit;

            INSERT INTO payroll_employee (payroll_run_id, employee_id, pit_rule_set_id, basic_amount, allowance_amount,
                                          bonus_amount, deduction_amount, taxable_amount, pit_amount, net_amount, row_status)
            VALUES (v_run, e.employee_id, v_set, e.base_salary, v_allow, v_bonus, v_ins, v_taxable, v_pit, v_net, 'PAID')
            RETURNING payroll_employee_id INTO v_pe;

            INSERT INTO payroll_line (payroll_employee_id, component_type, component_code, description, amount)
            VALUES (v_pe, 'EARNING', 'BASIC', 'Lương cơ bản theo hợp đồng', e.base_salary);
            INSERT INTO payroll_line (payroll_employee_id, component_type, component_code, description, amount)
            VALUES (v_pe, 'EARNING', 'ALLOWANCE', 'Phụ cấp ăn trưa và chức vụ', v_allow);
            IF v_bonus > 0 THEN
                INSERT INTO payroll_line (payroll_employee_id, component_type, component_code, description, amount)
                VALUES (v_pe, 'EARNING', 'BONUS', 'Thưởng chuyên cần và doanh số quầy', v_bonus);
            END IF;
            INSERT INTO payroll_line (payroll_employee_id, component_type, component_code, description, amount)
            VALUES (v_pe, 'DEDUCTION', 'INSURANCE', 'Bảo hiểm bắt buộc (10,5% lương cơ bản, mô phỏng)', v_ins);
            IF v_pit > 0 THEN
                INSERT INTO payroll_line (payroll_employee_id, component_type, component_code, description, amount)
                VALUES (v_pe, 'TAX', 'PIT', 'Thuế thu nhập cá nhân (mô phỏng)', v_pit);
            END IF;
        END LOOP;
    END LOOP;

    -- Ky luong thang hien tai dang mo, chua chay bang luong
    INSERT INTO payroll_period (chain_id, period_code, period_start, period_end, period_status)
    VALUES (v_chain, 'PR-' || TO_CHAR(TRUNC(SYSDATE, 'MM'), 'YYYYMM'), TRUNC(SYSDATE, 'MM'), LAST_DAY(TRUNC(SYSDATE, 'MM')), 'OPEN');
END;
/

-- 23. Cham soc khach hang: bai viet huong dan, yeu cau ho tro, hoi thoai va nhat ky tro ly AI
INSERT INTO knowledge_article (article_code, title, content, approval_status, valid_from, approved_by, approved_at)
SELECT a.code, a.title, a.content, 'APPROVED', SYSTIMESTAMP - INTERVAL '120' DAY(3), u.app_user_id,
       SYSTIMESTAMP - INTERVAL '120' DAY(3)
FROM (
    SELECT 'KB-REFUND-POLICY' code, 'Chính sách hoàn và đổi vé' title,
           'Vé đã thanh toán có thể gửi yêu cầu hoàn trước giờ chiếu ít nhất 2 tiếng. Tiền được hoàn về phương thức thanh toán ban đầu trong 3 đến 7 ngày làm việc. Vé đã quét tại cổng soát vé không được hoàn.' content FROM dual UNION ALL
    SELECT 'KB-TICKET-DELIVERY', 'Không nhận được vé điện tử',
           'Vé điện tử hiển thị ngay trong mục Vé của tôi sau khi thanh toán thành công. Nếu chưa thấy vé, vui lòng kiểm tra mã đơn hàng trong email xác nhận hoặc liên hệ bộ phận chăm sóc khách hàng.' FROM dual UNION ALL
    SELECT 'KB-INVOICE', 'Hướng dẫn xuất hóa đơn công ty',
           'Khách cần cung cấp tên công ty, mã số thuế và email nhận hóa đơn trong vòng 24 giờ kể từ khi thanh toán. Hóa đơn được gửi qua email sau khi nhà cung cấp xác nhận.' FROM dual UNION ALL
    SELECT 'KB-MEMBERSHIP', 'Quyền lợi thành viên THEFILM',
           'Mỗi 10.000 đồng chi tiêu được 1 điểm. Hạng Bạc từ 100 điểm, hạng Vàng từ 300 điểm, hạng Bạch kim từ 800 điểm. Điểm có hiệu lực 12 tháng kể từ ngày tích lũy.' FROM dual UNION ALL
    SELECT 'KB-SEAT-HOLD', 'Giữ ghế và thời gian thanh toán',
           'Ghế được giữ 10 phút kể từ khi tạo đơn. Hết thời gian giữ ghế đơn sẽ tự động hết hạn và ghế được trả lại cho khách khác.' FROM dual UNION ALL
    SELECT 'KB-PROMO-VOUCHER', 'Cách dùng mã giảm giá',
           'Nhập mã voucher ở bước thanh toán. Mỗi đơn chỉ áp dụng một mã, mỗi mã chỉ dùng một lần và không áp dụng cùng chương trình khác.' FROM dual
) a
CROSS JOIN (SELECT e.app_user_id FROM employee e WHERE e.employee_code = 'NV0004') u;

DECLARE
    v_n      PLS_INTEGER := 0;
    v_case   NUMBER;
    v_conv   NUMBER;
    v_msg    NUMBER;
    v_lead   NUMBER;
    v_agent  NUMBER;
    v_o      NUMBER;
    v_now    TIMESTAMP WITH TIME ZONE := SYSTIMESTAMP;

    FUNCTION cust(p_email VARCHAR2) RETURN NUMBER IS
        r NUMBER;
    BEGIN
        SELECT app_user_id INTO r FROM app_user WHERE email = p_email;
        RETURN r;
    END;

    FUNCTION ord(p_where VARCHAR2) RETURN NUMBER IS
        r NUMBER;
    BEGIN
        EXECUTE IMMEDIATE 'SELECT MIN(sales_order_id) FROM sales_order WHERE ' || p_where INTO r;
        RETURN r;
    END;

    FUNCTION ord_user(p_id NUMBER) RETURN NUMBER IS
        r NUMBER;
    BEGIN
        SELECT MAX(app_user_id) INTO r FROM sales_order WHERE sales_order_id = p_id;
        RETURN r;
    END;

    FUNCTION ord_cinema(p_id NUMBER, p_default VARCHAR2) RETURN VARCHAR2 IS
        r VARCHAR2(20);
    BEGIN
        SELECT MAX(c.cinema_code) INTO r FROM sales_order o JOIN cinema c ON c.cinema_id = o.cinema_id
        WHERE o.sales_order_id = p_id;
        RETURN NVL(r, p_default);
    END;

    PROCEDURE add_case(p_user NUMBER, p_cinema VARCHAR2, p_order NUMBER, p_cat VARCHAR2, p_prio VARCHAR2,
                       p_status VARCHAR2, p_subject VARCHAR2, p_assignee NUMBER, p_ago_hours NUMBER,
                       p_channel VARCHAR2, p_customer_msg VARCHAR2, p_ai_msg VARCHAR2, p_ai_intent VARCHAR2,
                       p_ai_kb VARCHAR2, p_handoff VARCHAR2, p_agent_msg VARCHAR2) IS
        v_open TIMESTAMP WITH TIME ZONE := v_now - NUMTODSINTERVAL(p_ago_hours, 'HOUR');
    BEGIN
        v_n := v_n + 1;
        INSERT INTO support_case (case_number, app_user_id, cinema_id, sales_order_id, case_category, priority_code,
                                  case_status, subject, assigned_to, opened_at, resolved_at, closed_at)
        VALUES ('SC' || TO_CHAR(v_open AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || LPAD(v_n, 4, '0'), p_user,
                (SELECT cinema_id FROM cinema WHERE cinema_code = p_cinema), p_order, p_cat, p_prio, p_status,
                p_subject, p_assignee, v_open,
                CASE WHEN p_status IN ('RESOLVED', 'CLOSED') THEN v_open + INTERVAL '3' HOUR END,
                CASE WHEN p_status = 'CLOSED' THEN v_open + INTERVAL '1' DAY END)
        RETURNING support_case_id INTO v_case;
        INSERT INTO support_conversation (support_case_id, channel_code, conversation_status, started_at, ended_at)
        VALUES (v_case, p_channel, CASE WHEN p_status IN ('RESOLVED', 'CLOSED') THEN 'CLOSED'
                                        WHEN p_status = 'WAITING_AGENT' THEN 'WAITING' ELSE 'OPEN' END,
                v_open, CASE WHEN p_status IN ('RESOLVED', 'CLOSED') THEN v_open + INTERVAL '3' HOUR END)
        RETURNING support_conversation_id INTO v_conv;
        INSERT INTO support_message (support_conversation_id, sender_type, sender_user_id, message_body, created_at)
        VALUES (v_conv, 'CUSTOMER', p_user, p_customer_msg, v_open);
        IF p_ai_msg IS NOT NULL THEN
            INSERT INTO support_message (support_conversation_id, sender_type, message_body, created_at)
            VALUES (v_conv, 'AI', p_ai_msg, v_open + INTERVAL '20' SECOND)
            RETURNING support_message_id INTO v_msg;
            INSERT INTO ai_interaction_log (support_message_id, provider_code, model_code, intent_code, confidence_score,
                                            grounding_reference, handoff_reason, interaction_status, duration_ms, created_at)
            VALUES (v_msg, 'MOCK_AI', 'faq-retriever-demo', p_ai_intent, CASE WHEN p_handoff IS NULL THEN 0.9140 ELSE 0.4120 END,
                    p_ai_kb, p_handoff, CASE WHEN p_handoff IS NULL THEN 'ANSWERED' ELSE 'HANDOFF' END,
                    900 + MOD(v_n * 137, 700), v_open + INTERVAL '21' SECOND);
        END IF;
        IF p_agent_msg IS NOT NULL THEN
            INSERT INTO support_message (support_conversation_id, sender_type, sender_user_id, message_body, created_at)
            VALUES (v_conv, 'AGENT', p_assignee, p_agent_msg, v_open + INTERVAL '40' MINUTE);
        END IF;
    END;
BEGIN
    SELECT app_user_id INTO v_lead FROM employee WHERE employee_code = 'NV0004';
    SELECT app_user_id INTO v_agent FROM employee WHERE employee_code = 'NV0005';

    add_case(cust('thanhmai.nguyen@example.com'), 'HCM-Q1', ord('order_status = ''ISSUED'' AND sales_channel = ''ONLINE'' AND app_user_id = ' || cust('thanhmai.nguyen@example.com')),
             'TICKET_DELIVERY', 'NORMAL', 'RESOLVED', 'Không thấy vé điện tử sau khi thanh toán', v_agent, 70, 'WEB',
             'Mình đã thanh toán xong nhưng vào mục Vé của tôi chưa thấy vé, nhờ bạn kiểm tra giúp.',
             'Vé điện tử xuất hiện trong mục Vé của tôi ngay sau thanh toán thành công. Bạn thử tải lại trang, nếu vẫn chưa thấy mình sẽ chuyển nhân viên hỗ trợ.',
             'TICKET_NOT_VISIBLE', 'KB-TICKET-DELIVERY', 'Khách vẫn chưa thấy vé sau khi tải lại trang',
             'Chào bạn, hệ thống đã đồng bộ lại vé cho đơn hàng của bạn. Bạn vui lòng tải lại trang và kiểm tra giúp mình nhé.');
    add_case(NULL, 'DN-HC', NULL, 'PAYMENT', 'URGENT', 'WAITING_AGENT', 'Bị trừ tiền nhưng đơn chưa có vé', NULL, 5, 'PHONE',
             'Tôi bị trừ tiền qua ví điện tử nhưng đơn vẫn báo chưa thanh toán, cần xử lý gấp trước giờ chiếu.',
             'Mình đã ghi nhận trường hợp bị trừ tiền nhưng đơn chưa xác nhận. Nhân viên sẽ kiểm tra giao dịch và phản hồi bạn sớm.',
             'PAYMENT_MISMATCH', 'KB-SEAT-HOLD', 'Cần đối soát giao dịch thanh toán thủ công', NULL);
    add_case(cust('bichphuong.le@example.com'), 'HCM-TD', NULL, 'POLICY', 'LOW', 'CLOSED', 'Hỏi chính sách hoàn vé', NULL, 150, 'MOBILE',
             'Nếu mình không đi được thì có hoàn vé không, hoàn trước bao lâu?',
             'Bạn có thể gửi yêu cầu hoàn vé trước giờ chiếu ít nhất 2 tiếng. Tiền hoàn về phương thức thanh toán ban đầu trong 3 đến 7 ngày làm việc.',
             'REFUND_POLICY', 'KB-REFUND-POLICY', NULL, NULL);
    add_case(cust('thuhien.hoang@example.com'), 'HN-CG', NULL, 'FEEDBACK', 'LOW', 'NEW', 'Góp ý về âm thanh phòng chiếu', NULL, 20, 'EMAIL',
             'Âm thanh ở phòng P02 hôm qua hơi rè ở loa bên trái, mong rạp kiểm tra lại giúp.', NULL, NULL, NULL, NULL, NULL);
    v_o := ord('order_status = ''REFUND_PENDING''');
    add_case(NVL(ord_user(v_o), cust('long.dang00@example.com')), ord_cinema(v_o, 'HN-CG'), v_o, 'REFUND', 'HIGH', 'IN_PROGRESS',
             'Yêu cầu hoàn vé đã gửi nhưng chưa được duyệt', v_lead, 9, 'WEB',
             'Mình đã gửi yêu cầu hoàn vé từ sáng nhưng chưa thấy cập nhật trạng thái.',
             'Yêu cầu hoàn vé cần được quản lý rạp duyệt. Mình đã chuyển trường hợp của bạn cho nhân viên hỗ trợ để xử lý nhanh hơn.',
             'REFUND_STATUS', 'KB-REFUND-POLICY', 'Yêu cầu hoàn tiền đang chờ duyệt, cần nhân viên theo dõi',
             'Chào bạn, mình đang phối hợp với quản lý rạp để duyệt yêu cầu hoàn vé và sẽ báo kết quả trong hôm nay.');
    v_o := ord('EXISTS (SELECT 1 FROM tax_invoice i WHERE i.sales_order_id = sales_order.sales_order_id AND i.invoice_status = ''ACCEPTED'')');
    add_case(NVL(ord_user(v_o), cust('hung.tran92@example.com')), ord_cinema(v_o, 'HCM-Q1'), v_o,
             'INVOICE', 'NORMAL', 'RESOLVED', 'Cần xuất hóa đơn công ty', v_agent, 100, 'EMAIL',
             'Công ty mình cần hóa đơn cho đơn hàng vừa mua, thông tin công ty mình gửi kèm bên dưới.',
             'Bạn vui lòng cung cấp tên công ty, mã số thuế và email nhận hóa đơn. Nhân viên sẽ hỗ trợ xuất hóa đơn cho bạn.',
             'INVOICE_REQUEST', 'KB-INVOICE', 'Cần nhân viên kiểm tra thông tin người mua', 'Hóa đơn đã được phát hành và gửi về email của bạn.');
END;
/

-- 24. Cau hinh he thong, nhat ky thao tac, hang doi su kien tich hop, bao tri ghe
INSERT INTO app_setting (setting_key, scope_type, scope_id, value_type, setting_value, valid_from, description, updated_by)
SELECT s.k, 'GLOBAL', '0', s.t, s.v, SYSTIMESTAMP - INTERVAL '90' DAY, s.d, u.app_user_id
FROM (
    SELECT 'booking.hold_seconds' k, 'NUMBER' t, '600' v, 'Thời gian giữ ghế khi tạo đơn (giây)' d FROM dual UNION ALL
    SELECT 'booking.max_seats_per_order', 'NUMBER', '8', 'Số ghế tối đa trong một đơn' FROM dual UNION ALL
    SELECT 'payment.default_provider', 'STRING', 'MOCK', 'Cổng thanh toán mặc định (mô phỏng)' FROM dual UNION ALL
    SELECT 'tax.vat.default_percent', 'NUMBER', '8', 'Thuế suất VAT mô phỏng dùng khi tính đơn' FROM dual UNION ALL
    SELECT 'loyalty.vnd_per_point', 'NUMBER', '10000', 'Số tiền chi tiêu để được 1 điểm thành viên' FROM dual UNION ALL
    SELECT 'support.ai.enabled', 'BOOLEAN', 'true', 'Bật trợ lý AI trả lời câu hỏi thường gặp' FROM dual UNION ALL
    SELECT 'ticket.refund_cutoff_minutes', 'NUMBER', '120', 'Hoàn vé phải gửi trước giờ chiếu tối thiểu (phút)' FROM dual
) s
CROSS JOIN (SELECT e.app_user_id FROM employee e WHERE e.employee_code = 'NV0001') u;

INSERT INTO app_setting (setting_key, scope_type, scope_id, value_type, setting_value, valid_from, description, updated_by)
SELECT 'screening.cleanup_minutes', 'CINEMA', TO_CHAR(c.cinema_id), 'NUMBER', '15', SYSTIMESTAMP - INTERVAL '90' DAY,
       'Thời gian dọn phòng giữa hai suất chiếu', u.app_user_id
FROM cinema c CROSS JOIN (SELECT e.app_user_id FROM employee e WHERE e.employee_code = 'NV0001') u
WHERE c.cinema_code IN ('HCM-Q1', 'HCM-TD', 'HN-CG', 'DN-HC');

INSERT INTO audit_event (actor_user_id, cinema_id, action_code, object_type, object_id, trace_id, before_data, after_data,
                         source_ip, occurred_at)
SELECT u.app_user_id, c.cinema_id, a.action_code, a.object_type, a.object_id,
       LOWER(SUBSTR(RAWTOHEX(STANDARD_HASH('trace' || a.object_id || a.action_code, 'SHA256')), 1, 32)),
       a.before_data, a.after_data, a.ip, SYSTIMESTAMP - NUMTODSINTERVAL(a.hours_ago, 'HOUR')
FROM (
    SELECT 'NV0001' emp, 'HCM-Q1' cinema, 'PRICE_RULE_APPROVE' action_code, 'PRICE_RULE' object_type, 'PR-ALL-STANDARD' object_id,
           '{"approvalStatus":"PENDING"}' before_data, '{"approvalStatus":"APPROVED"}' after_data, '10.10.1.11' ip, 1400 hours_ago FROM dual UNION ALL
    SELECT 'NV0001', 'HCM-Q1', 'FILM_CREATE', 'FILM', 'TF011', NULL, '{"filmCode":"TF011","status":"APPROVED"}', '10.10.1.11', 700 FROM dual UNION ALL
    SELECT 'NV0101', 'HCM-Q1', 'SCREENING_CREATE', 'SCREENING', 'HCM-Q1/P01', NULL, '{"status":"ON_SALE"}', '10.10.2.21', 240 FROM dual UNION ALL
    SELECT 'NV0201', 'HCM-TD', 'ROOM_UPDATE', 'ROOM', 'HCM-TD/P02', '{"status":"ACTIVE"}', '{"status":"ACTIVE","note":"Thay loa kênh trái"}', '10.10.3.21', 96 FROM dual UNION ALL
    SELECT 'NV0301', 'HN-CG', 'REFUND_APPROVE', 'REFUND_REQUEST', 'HN-CG', '{"requestStatus":"PENDING"}', '{"requestStatus":"APPROVED"}', '10.10.4.21', 30 FROM dual UNION ALL
    SELECT 'NV0002', 'HCM-Q1', 'EMPLOYEE_CONTRACT_RENEW', 'EMPLOYEE_CONTRACT', 'HD-NV0102', '{"validTo":"2026-09-30"}', '{"validTo":"2027-12-31"}', '10.10.1.15', 500 FROM dual UNION ALL
    SELECT 'NV0003', 'HCM-Q1', 'PAYROLL_RUN_APPROVE', 'PAYROLL_RUN', 'PR-LAST', '{"runStatus":"REVIEW"}', '{"runStatus":"PAID"}', '10.10.1.13', 240 FROM dual
) a
JOIN employee e ON e.employee_code = a.emp
JOIN app_user u ON u.app_user_id = e.app_user_id
JOIN cinema c ON c.cinema_code = a.cinema;

INSERT INTO integration_outbox (aggregate_type, aggregate_id, event_type, idempotency_key, payload, event_status,
                                attempt_count, available_at, created_at, processed_at, last_error)
SELECT 'SALES_ORDER', x.order_number, 'ORDER_ISSUED', 'order-issued-' || x.order_number,
       '{"orderNumber":"' || x.order_number || '","totalAmount":' || TRUNC(x.total_amount) || ',"cinemaId":' || x.cinema_id || '}',
       CASE WHEN x.rn <= 20 THEN 'PROCESSED' WHEN x.rn <= 22 THEN 'PENDING' WHEN x.rn = 23 THEN 'RETRY' ELSE 'DEAD' END,
       CASE WHEN x.rn <= 20 THEN 1 WHEN x.rn <= 22 THEN 0 WHEN x.rn = 23 THEN 2 ELSE 5 END,
       x.paid_at, x.paid_at,
       CASE WHEN x.rn <= 20 THEN x.paid_at + INTERVAL '3' SECOND END,
       CASE WHEN x.rn = 23 THEN 'Hệ thống kế toán tạm thời không phản hồi (HTTP 503).'
            WHEN x.rn >= 24 THEN 'Không gửi được sau 5 lần thử, cần kiểm tra thủ công.' END
FROM (
    SELECT o.order_number, o.total_amount, o.cinema_id, o.paid_at, ROW_NUMBER() OVER (ORDER BY o.paid_at DESC) rn
    FROM sales_order o WHERE o.order_status = 'ISSUED'
) x
WHERE x.rn <= 24;

INSERT INTO seat_maintenance (room_seat_id, start_at, end_at, reason, status, reported_by, resolved_by, created_at)
SELECT rs.room_seat_id, SYSTIMESTAMP - INTERVAL '40' DAY, SYSTIMESTAMP - INTERVAL '38' DAY, m.reason, 'RESOLVED',
       rep.app_user_id, rep.app_user_id, SYSTIMESTAMP - INTERVAL '40' DAY
FROM (
    SELECT 'HCM-Q1' cinema, 'P01' room, 'A1' seat, 'Gãy tay vịn, đã thay mới.' reason, 'NV0101' emp FROM dual UNION ALL
    SELECT 'HN-CG', 'P01', 'B3', 'Mô tơ ngả lưng ghế hỏng, đã sửa.', 'NV0301' FROM dual
) m
JOIN cinema c ON c.cinema_code = m.cinema
JOIN cinema_room r ON r.cinema_id = c.cinema_id AND r.room_code = m.room
JOIN room_layout l ON l.room_id = r.room_id AND l.status = 'ACTIVE'
JOIN room_seat rs ON rs.layout_id = l.layout_id AND rs.seat_label = m.seat
JOIN employee e ON e.employee_code = m.emp
JOIN app_user rep ON rep.app_user_id = e.app_user_id;
