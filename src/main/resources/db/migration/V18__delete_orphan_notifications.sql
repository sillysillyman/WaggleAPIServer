-- soft-delete된 team을 가리키는 알림 정리
DELETE n FROM notifications n
JOIN teams t ON CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.teamId')) AS UNSIGNED) = t.id
WHERE t.deleted_at IS NOT NULL;

-- soft-delete된 post를 가리키는 알림 정리
DELETE n FROM notifications n
JOIN posts p ON CAST(JSON_UNQUOTE(JSON_EXTRACT(n.metadata, '$.postId')) AS UNSIGNED) = p.id
WHERE p.deleted_at IS NOT NULL;
