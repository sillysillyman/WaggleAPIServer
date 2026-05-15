-- post가 soft-delete됐지만 application이 살아있는 좀비 정리
-- UserService.deactivateUser에서 post 작성자 탈퇴 시 외부인 application을 cascade하지 않아 발생
UPDATE applications a
JOIN posts p ON p.id = a.post_id
SET a.deleted_at = p.deleted_at
WHERE p.deleted_at IS NOT NULL AND a.deleted_at IS NULL;
