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
INSERT INTO cinema_room VALUES (1, 1, 1, 'P01', 'Phòng 01', 'ACTIVE');
INSERT INTO cinema_room VALUES (2, 1, 2, 'VIP01', 'Phòng VIP 01', 'ACTIVE');
INSERT INTO cinema_room VALUES (3, 2, 1, 'P01', 'Phòng 01', 'ACTIVE');

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

INSERT INTO show_seat VALUES (1, 1, 1, 'AVAILABLE');
INSERT INTO show_seat VALUES (2, 1, 2, 'AVAILABLE');
INSERT INTO show_seat VALUES (3, 1, 3, 'SOLD');
INSERT INTO show_seat VALUES (4, 2, 4, 'AVAILABLE');
INSERT INTO show_seat VALUES (5, 2, 5, 'HELD');