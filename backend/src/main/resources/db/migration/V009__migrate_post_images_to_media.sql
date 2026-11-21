INSERT INTO post_media (
    post_id,
    url,
    type,
    position
)
SELECT
    id,
    image_url,
    'IMAGE',
    0
FROM post
WHERE image_url IS NOT NULL
  AND TRIM(image_url) <> '';