UPDATE app_user
SET password = display_name
WHERE password IS NULL OR BTRIM(password) = '';
