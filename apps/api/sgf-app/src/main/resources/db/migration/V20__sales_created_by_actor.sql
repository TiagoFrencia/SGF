-- V20: align sales.created_by with the current actor username model.

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(100);

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS created_by_text VARCHAR(255);

UPDATE sales s
SET created_by_text = COALESCE(u.username, s.created_by::text)
FROM users u
WHERE s.created_by_text IS NULL
  AND u.id = s.created_by;

UPDATE sales
SET created_by_text = created_by::text
WHERE created_by_text IS NULL;

DO $$
DECLARE
    fk_name text;
BEGIN
    SELECT tc.constraint_name
    INTO fk_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
     AND tc.table_schema = kcu.table_schema
    WHERE tc.table_schema = 'public'
      AND tc.table_name = 'sales'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'created_by'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE sales DROP CONSTRAINT %I', fk_name);
    END IF;
END $$;

ALTER TABLE sales
    DROP COLUMN created_by;

ALTER TABLE sales
    RENAME COLUMN created_by_text TO created_by;

ALTER TABLE sales
    ALTER COLUMN created_by SET NOT NULL;
