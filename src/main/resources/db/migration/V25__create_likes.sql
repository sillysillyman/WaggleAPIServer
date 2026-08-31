-- 모집글·댓글 좋아요. 대상은 (type, target_id)로 지정하며 FK 미설정 (bookmarks 전례, 정리는 도메인 이벤트 담당).
-- 토글 OFF는 hard delete. soft delete가 불필요한 관계 데이터라 deleted_at 미보유.
CREATE TABLE likes
(
    type       VARCHAR(20) NOT NULL,
    target_id  BIGINT      NOT NULL,
    user_id    BINARY(16)  NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (type, target_id, user_id)
);

-- 탈퇴 cascade와 "내가 누른 좋아요" 배치 조회용.
-- InnoDB가 PK 나머지(type, target_id)를 순서대로 덧붙여 실질 (user_id, type, target_id) 커버링 인덱스가 됨.
CREATE INDEX idx_likes_user ON likes (user_id);
