-- Allow an approved price rule to target one film, optionally combined with
-- cinema, room type, seat type and screening type.
ALTER TABLE price_rule ADD film_id NUMBER(19);

ALTER TABLE price_rule ADD CONSTRAINT fk_price_rule_film
    FOREIGN KEY (film_id) REFERENCES film (film_id);

CREATE INDEX ix_price_rule_film_match
    ON price_rule (film_id, approval_status, screening_type_code, valid_from, valid_to);

COMMENT ON COLUMN price_rule.film_id IS 'Phim áp dụng riêng; NULL nghĩa là áp dụng cho mọi phim';
