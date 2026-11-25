-- Move the previously shared room-type catalog into each owning cinema chain.
ALTER TABLE room_type ADD (chain_id NUMBER(19));
ALTER TABLE room_type DROP CONSTRAINT uq_room_type_code;

DECLARE
    v_anchor_chain_id NUMBER;
    v_type_count NUMBER;
BEGIN
    SELECT MIN(chain_id) INTO v_anchor_chain_id FROM org_chain;
    SELECT COUNT(*) INTO v_type_count FROM room_type;
    IF v_anchor_chain_id IS NULL AND v_type_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Cannot scope existing room types because no cinema chain exists. Create a chain and rerun this migration.');
    END IF;
    IF v_anchor_chain_id IS NOT NULL THEN
        UPDATE room_type SET chain_id = v_anchor_chain_id WHERE chain_id IS NULL;

        -- Copy the old shared catalog to every other chain before rewiring rooms.
        INSERT INTO room_type (chain_id, type_code, type_name, description, attributes_json, status)
        SELECT c.chain_id, t.type_code, t.type_name, t.description, t.attributes_json, t.status
          FROM org_chain c
          CROSS JOIN room_type t
         WHERE c.chain_id <> v_anchor_chain_id;
    END IF;
END;
/

MERGE INTO cinema_room target
USING (
    SELECT room.room_id, chain_type.room_type_id
      FROM cinema_room room
      JOIN cinema c ON c.cinema_id = room.cinema_id
      JOIN room_type old_type ON old_type.room_type_id = room.room_type_id
      JOIN room_type chain_type
        ON chain_type.chain_id = c.chain_id
       AND chain_type.type_code = old_type.type_code
) mapped
ON (target.room_id = mapped.room_id)
WHEN MATCHED THEN UPDATE SET target.room_type_id = mapped.room_type_id;

ALTER TABLE room_type MODIFY (chain_id NOT NULL);
ALTER TABLE room_type ADD CONSTRAINT fk_room_type_chain
    FOREIGN KEY (chain_id) REFERENCES org_chain (chain_id);
ALTER TABLE room_type ADD CONSTRAINT uq_room_type_chain_code
    UNIQUE (chain_id, type_code);

INSERT INTO room_type (chain_id, type_code, type_name, description)
SELECT c.chain_id, 'STANDARD', UNISTR('Ph\00F2ng ti\00EAu chu\1EA9n'),
       UNISTR('Ph\1ED5 bi\1EBFn cho su\1EA5t chi\1EBFu th\00F4ng th\01B0\1EDDng.')
  FROM org_chain c
 WHERE NOT EXISTS (SELECT 1 FROM room_type t WHERE t.chain_id = c.chain_id AND t.type_code = 'STANDARD');
INSERT INTO room_type (chain_id, type_code, type_name, description)
SELECT c.chain_id, 'PREMIUM', UNISTR('Ph\00F2ng cao c\1EA5p'),
       UNISTR('D\1ECBch v\1EE5 cao h\01A1n; ti\1EC7n \00EDch c\1EE5 th\1EC3 do r\1EA1p c\1EA5u h\00ECnh.')
  FROM org_chain c
 WHERE NOT EXISTS (SELECT 1 FROM room_type t WHERE t.chain_id = c.chain_id AND t.type_code = 'PREMIUM');
INSERT INTO room_type (chain_id, type_code, type_name, description)
SELECT c.chain_id, 'LARGE_FORMAT', UNISTR('Ph\00F2ng \0111\1ECBnh d\1EA1ng l\1EDBn'),
       UNISTR('M\00E0n h\00ECnh l\1EDBn; c\00F4ng ngh\1EC7 c\1EE5 th\1EC3 do r\1EA1p c\1EA5u h\00ECnh.')
  FROM org_chain c
 WHERE NOT EXISTS (SELECT 1 FROM room_type t WHERE t.chain_id = c.chain_id AND t.type_code = 'LARGE_FORMAT');

COMMENT ON COLUMN room_type.chain_id IS 'Chuỗi sở hữu danh mục loại phòng chiếu; mỗi chuỗi có danh mục riêng';
COMMIT;
