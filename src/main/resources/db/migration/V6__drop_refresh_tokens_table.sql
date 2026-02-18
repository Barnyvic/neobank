-- Refresh tokens are now stored in Redis; remove DB table.
DROP TABLE IF EXISTS refresh_tokens;
