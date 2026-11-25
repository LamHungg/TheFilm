-- Give MoMo demo films a real Vietnamese 2D version so they can be scheduled.
-- The merge is safe for existing installs where an administrator already added this version.
MERGE INTO film_version target
USING (
    SELECT f.film_id, f.runtime_minutes
    FROM film f
    WHERE f.film_code IN (
        'DEMO-MOMO-001',
        'DEMO-MOMO-002',
        'DEMO-MOMO-003',
        'DEMO-MOMO-004'
    )
) source
ON (target.film_id = source.film_id AND target.version_code = '2D_VN')
WHEN NOT MATCHED THEN INSERT (
    film_id, version_code, language_code, subtitle_code, format_code, runtime_minutes, status
) VALUES (
    source.film_id, '2D_VN', 'vi', 'vi', '2D', source.runtime_minutes, 'ACTIVE'
);

