-- 모집글 댓글. parent_id가 NULL이면 최상위, 아니면 답글(1-depth는 서비스에서 강제).
-- 삭제 상태는 두 컬럼으로 분리:
--   deleted_at    = 행이 없는 것으로 취급 (전역 soft delete 필터가 걸러냄)
--   tombstoned_at = 본문만 감추고 답글 스레드 앵커로 남김 (필터 통과)
CREATE TABLE comments
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id       BIGINT        NOT NULL,
    user_id       BINARY(16)    NOT NULL,
    parent_id     BIGINT        NULL,
    content       VARCHAR(1000) NOT NULL,
    tombstoned_at DATETIME(6)   NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    deleted_at    DATETIME(6)   NULL
);

-- 최상위 조회(post_id = ? AND parent_id IS NULL), 답글 조회(post_id = ? AND parent_id IN (...)),
-- 답글 존재 확인을 모두 커버. equality prefix 뒤에 PK(id)가 붙어 ORDER BY id가 filesort 없이 처리됨.
CREATE INDEX idx_comments_post_parent ON comments (post_id, parent_id);

CREATE INDEX idx_comments_user ON comments (user_id);
