-- 도메인 룰 강화: 한 모집글에 사용자당 1지원만 (position 컬럼은 인덱스에서 제외)
ALTER TABLE applications
    DROP INDEX idx_applications_post_user_position;

CREATE INDEX idx_applications_post_user
    ON applications (post_id, user_id);
