ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);

UPDATE app_user
SET password = display_name
WHERE password IS NULL OR BTRIM(password) = '';

ALTER TABLE app_user
    ALTER COLUMN password SET NOT NULL;
