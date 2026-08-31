-- 조회 시점에는 Redis에만 증분을 쌓고 스케줄러가 주기적으로 이 컬럼에 반영함
ALTER TABLE posts
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
