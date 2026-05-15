-- application이 soft-delete됐지만 application_read가 살아있는 좀비 정리
-- 모든 application 삭제 경로에서 application_read cascade가 누락되어 발생
UPDATE application_reads ar
JOIN applications a ON a.id = ar.application_id
SET ar.deleted_at = a.deleted_at
WHERE a.deleted_at IS NOT NULL AND ar.deleted_at IS NULL;
