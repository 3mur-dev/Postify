UPDATE messages
SET read_at = DATE_SUB(read_at, INTERVAL 3 HOUR)
WHERE read_at IS NOT NULL;