-- Add richer movie metadata and sample MoMo Cinema reference data.
-- Apply once to an existing Oracle schema after V005. Adds separate DEMO-MOMO records and
-- does not overwrite existing TF001-... sample rows. External images/ratings are demo references;
-- Rating values are a snapshot observed on 2026-11-16, not a live MoMo feed.
-- External images are demo references; copy poster files to THEFILM media storage before production use.
ALTER TABLE film ADD (
    release_date DATE,
    director VARCHAR2(300 CHAR),
    cast_names VARCHAR2(2000 CHAR),
    external_rating_score NUMBER(3,1),
    external_rating_count NUMBER(10),
    external_rating_source VARCHAR2(80 CHAR),
    external_rating_uri VARCHAR2(1000 CHAR),
    external_rating_updated_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE film ADD CONSTRAINT ck_film_ext_rating
    CHECK (external_rating_score IS NULL OR external_rating_score BETWEEN 0 AND 10);
ALTER TABLE film ADD CONSTRAINT ck_film_ext_rating_count
    CHECK (external_rating_count IS NULL OR external_rating_count >= 0);

DECLARE
    PROCEDURE update_demo_film(
        p_code VARCHAR2, p_title VARCHAR2, p_original VARCHAR2, p_synopsis VARCHAR2,
        p_runtime NUMBER, p_age_code VARCHAR2, p_release_date DATE, p_director VARCHAR2,
        p_cast VARCHAR2, p_poster VARCHAR2, p_trailer VARCHAR2, p_score NUMBER,
        p_rating_count NUMBER, p_rating_uri VARCHAR2, p_genres VARCHAR2
    ) IS
        v_film_id film.film_id%TYPE;
        v_age_id age_classification.age_classification_id%TYPE;
    BEGIN
        SELECT age_classification_id INTO v_age_id
        FROM age_classification WHERE classification_code = p_age_code;
        MERGE INTO film f
        USING (SELECT p_code film_code FROM dual) src
        ON (f.film_code = src.film_code)
        WHEN MATCHED THEN UPDATE SET
            f.title_local = p_title, f.title_original = p_original, f.synopsis = p_synopsis,
            f.runtime_minutes = p_runtime, f.age_classification_id = v_age_id, f.poster_uri = p_poster,
            f.trailer_uri = p_trailer, f.release_date = p_release_date, f.director = p_director,
            f.cast_names = p_cast, f.external_rating_score = p_score, f.external_rating_count = p_rating_count,
            f.external_rating_source = 'MoMo Cinema', f.external_rating_uri = p_rating_uri,
            f.external_rating_updated_at = TO_TIMESTAMP_TZ(
                '2026-11-16 00:00:00 +07:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')
        WHEN NOT MATCHED THEN INSERT (
            film_code, title_local, title_original, synopsis, runtime_minutes, age_classification_id,
            poster_uri, trailer_uri, release_date, director, cast_names, external_rating_score,
            external_rating_count, external_rating_source, external_rating_uri,
            external_rating_updated_at, approval_status
        ) VALUES (
            p_code, p_title, p_original, p_synopsis, p_runtime, v_age_id, p_poster, p_trailer,
            p_release_date, p_director, p_cast, p_score, p_rating_count, 'MoMo Cinema', p_rating_uri,
            TO_TIMESTAMP_TZ('2026-11-16 00:00:00 +07:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM'), 'APPROVED'
        );
        SELECT film_id INTO v_film_id FROM film WHERE film_code = p_code;
        DELETE FROM film_genre_map WHERE film_id = v_film_id;
        FOR g IN (
            SELECT TRIM(REGEXP_SUBSTR(p_genres, '[^,]+', 1, LEVEL)) code
            FROM dual CONNECT BY REGEXP_SUBSTR(p_genres, '[^,]+', 1, LEVEL) IS NOT NULL
        ) LOOP
            INSERT INTO film_genre_map (film_id, genre_id)
            SELECT v_film_id, genre_id FROM film_genre WHERE genre_code = g.code;
        END LOOP;
    END;
BEGIN
    update_demo_film('DEMO-MOMO-001', 'Ngày Con Còn Mẹ', 'Ngày Con Còn Mẹ',
        'Một gia đình đối diện những tổn thương cũ và tìm cách hàn gắn khi biến cố sức khỏe làm thay đổi vai trò của mỗi người.',
        113, 'T16', DATE '2026-11-11', 'Vũ Ngọc Đãng',
        'Hồng Đào, Phương Anh Đào, Trần Huy Anh, Nguyên Thảo, Ma Ran Đô, Quỳnh Lương',
        'https://cinema.momocdn.net/convert-webp/img/4986547019998252-1200wx1600h.jpg?size=S',
        'https://www.youtube.com/watch?v=c4zgZ2s5N_Y', 9, 1142,
        'https://www.momo.vn/cinema/ngay-con-con-me-25093', 'FAMILY,DRAMA');
    update_demo_film('DEMO-MOMO-002', 'Bò Sữa Bay', 'Flying Cows',
        'Một nhóm anh em lên kế hoạch đánh cắp công thức sữa bí mật, rồi bị cuốn vào những lựa chọn giữa gia đình và tình cảm.',
        108, 'T18', DATE '2026-11-06', 'Nguyễn Phạm Thành Đạt',
        'Nguyễn Hùng, Ngọc Xuân, Võ Điền Gia Huy, Trần Tiểu Vy, Trần Gia Huy, Lãnh Thanh',
        'https://cinema.momocdn.net/convert-webp/img/4904564921820363-bsb_1200x1600.png?size=S',
        'https://www.youtube.com/watch?v=2Y8yw8bAG1o', 10, 1744,
        'https://www.momo.vn/cinema/bo-sua-bay-25058', 'DRAMA');
    update_demo_film('DEMO-MOMO-003', 'Godzilla Trừ Không', 'Godzilla Minus Zero',
        'Trong một xã hội hậu chiến đang tái thiết, sự trở lại của Godzilla đẩy con người vào một cuộc chiến sinh tồn mới.',
        135, 'T13', DATE '2026-11-06', 'Takashi Yamazaki',
        'Ryunosuke Kamiki, Minami Hamabe',
        'https://cinema.momocdn.net/convert-webp/img/2404819473541948-godzzzzz.jpg?size=S',
        'https://www.youtube.com/watch?v=Xmy-Z3dHMd8', 10, 1996,
        'https://www.momo.vn/cinema/godzilla-tru-khong-25137', 'SCIFI,ACTION,DRAMA');
    update_demo_film('DEMO-MOMO-004', 'Trại Giam Hạnh Phúc', 'The Happiest Cell',
        'Một người thầy phải thích nghi với cuộc sống trong buồng giam và tìm cách giữ lòng nhân hậu giữa những xung đột.',
        123, 'T16', DATE '2026-11-20', 'Nguyễn Nhất Trung',
        'Tuấn Trần, Lương Thế Thành, Ma Ran Đô, Tiến Luật, La Thành, Lê Quốc Thuận, Hoàng Phi, Võ Văn Điền, Khuyến Dương, Gi-A Nguyễn',
        'https://cinema.momocdn.net/convert-webp/img/3867294930842850-tghp.jpg?size=S',
        'https://www.youtube.com/watch?v=rG3MuGSBY5k', 10, 26,
        'https://www.momo.vn/cinema/trai-giam-hanh-phuc-25162', 'DRAMA,FAMILY,COMEDY');
END;
/
COMMIT;
