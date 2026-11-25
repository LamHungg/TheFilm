ALTER TABLE fnb_product
    ADD low_stock_threshold NUMBER(14,3) DEFAULT 10 NOT NULL;

COMMENT ON COLUMN fnb_product.low_stock_threshold IS
    'Số lượng tồn khả dụng theo từng sản phẩm dùng phát cảnh báo sắp hết hàng';

MERGE INTO fnb_category target
USING (
    SELECT 'POPCORN' category_code, CAST(UNISTR('B\1EAFp rang') AS VARCHAR2(150 CHAR)) category_name FROM dual UNION ALL
    SELECT 'DRINK', CAST(UNISTR('N\01B0\1EDBc u\1ED1ng') AS VARCHAR2(150 CHAR)) FROM dual UNION ALL
    SELECT 'SNACK', CAST(UNISTR('\0110\1ED3 \0103n nh\1EB9') AS VARCHAR2(150 CHAR)) FROM dual UNION ALL
    SELECT 'COMBO', 'Combo' FROM dual
) source
ON (target.category_code = source.category_code)
WHEN NOT MATCHED THEN INSERT (category_code, category_name, status)
    VALUES (source.category_code, source.category_name, 'ACTIVE');
