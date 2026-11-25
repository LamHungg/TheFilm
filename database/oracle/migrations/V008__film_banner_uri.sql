-- Cho phép quản trị lưu ảnh nền riêng, không dùng poster làm nền hero.
ALTER TABLE film ADD (banner_uri VARCHAR2(1000 CHAR));

COMMENT ON COLUMN film.banner_uri IS 'Đường dẫn ảnh nền phim dùng trên trang chi tiết';

UPDATE film
SET banner_uri = 'https://homepage.momocdn.net/img/ngay-con-con-me-261104142117.webp?size=XL'
WHERE film_code = 'DEMO-MOMO-001'
  AND banner_uri IS NULL;
