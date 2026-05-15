-- post가 soft-delete됐지만 가리키는 POST bookmark가 살아있는 좀비 정리
-- TeamService.deleteTeam, UserService.deactivateUser에서 POST bookmark cascade가 누락되어 발생
DELETE b FROM bookmarks b
JOIN posts p ON p.id = b.target_id
WHERE b.type = 'POST' AND p.deleted_at IS NOT NULL;
