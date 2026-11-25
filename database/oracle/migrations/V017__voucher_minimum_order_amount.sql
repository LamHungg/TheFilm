ALTER TABLE promo_campaign
    ADD minimum_order_amount NUMBER(18,2) DEFAULT 0 NOT NULL;

ALTER TABLE promo_campaign
    ADD CONSTRAINT ck_promo_min_order CHECK (minimum_order_amount >= 0);
