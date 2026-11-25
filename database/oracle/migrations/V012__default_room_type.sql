-- Ensure existing installations have the standard room type catalog.
INSERT INTO room_type (type_code, type_name, description)
SELECT 'STANDARD', UNISTR('Ph\00F2ng ti\00EAu chu\1EA9n'),
       UNISTR('Ph\1ED5 bi\1EBFn cho su\1EA5t chi\1EBFu th\00F4ng th\01B0\1EDDng.')
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM room_type WHERE type_code = 'STANDARD'
);
INSERT INTO room_type (type_code, type_name, description)
SELECT 'PREMIUM', UNISTR('Ph\00F2ng cao c\1EA5p'),
       UNISTR('D\1ECBch v\1EE5 cao h\01A1n; ti\1EC7n \00EDch c\1EE5 th\1EC3 do r\1EA1p c\1EA5u h\00ECnh.')
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM room_type WHERE type_code = 'PREMIUM'
);
INSERT INTO room_type (type_code, type_name, description)
SELECT 'LARGE_FORMAT', UNISTR('Ph\00F2ng \0111\1ECBnh d\1EA1ng l\1EDBn'),
       UNISTR('M\00E0n h\00ECnh l\1EDBn; c\00F4ng ngh\1EC7 c\1EE5 th\1EC3 do r\1EA1p c\1EA5u h\00ECnh.')
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM room_type WHERE type_code = 'LARGE_FORMAT'
);

UPDATE room_type
SET type_name = UNISTR('Ph\00F2ng ti\00EAu chu\1EA9n'),
    description = UNISTR('Ph\1ED5 bi\1EBFn cho su\1EA5t chi\1EBFu th\00F4ng th\01B0\1EDDng.')
WHERE type_code = 'STANDARD';
UPDATE room_type
SET type_name = UNISTR('Ph\00F2ng cao c\1EA5p'),
    description = UNISTR('D\1ECBch v\1EE5 cao h\01A1n; ti\1EC7n \00EDch c\1EE5 th\1EC3 do r\1EA1p c\1EA5u h\00ECnh.')
WHERE type_code = 'PREMIUM';
UPDATE room_type
SET type_name = UNISTR('Ph\00F2ng \0111\1ECBnh d\1EA1ng l\1EDBn'),
    description = UNISTR('M\00E0n h\00ECnh l\1EDBn; c\00F4ng ngh\1EC7 c\1EE5 th\1EC3 do r\1EA1p c\1EA5u h\00ECnh.')
WHERE type_code = 'LARGE_FORMAT';

COMMIT;
