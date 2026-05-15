-- @ElementCollection child 테이블 FK 전부 ON DELETE CASCADE로 통일
-- @ElementCollection은 부모 entity가 owned하는 collection이라 부모 삭제 시 child도 함께 정리되어야 함
-- 특히 recruitments는 hard delete되므로 cascade 없으면 FK 위반 (TeamService.deleteTeam 경로)
-- 나머지 부모(users, applications, member_reviews)는 현재 soft delete만 되지만 일관성을 위해 함께 적용
ALTER TABLE recruitment_skills
    DROP FOREIGN KEY FKfi7fj762k809ttm5n0hee5oft,
    ADD CONSTRAINT FKfi7fj762k809ttm5n0hee5oft
        FOREIGN KEY (recruitment_id) REFERENCES recruitments (id) ON DELETE CASCADE;

ALTER TABLE user_skills
    DROP FOREIGN KEY FKro13if9r7fwkr5115715127ai,
    ADD CONSTRAINT FKro13if9r7fwkr5115715127ai
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_portfolios
    DROP FOREIGN KEY FK1y44v9x6f4ts23ovfpxi0qnqm,
    ADD CONSTRAINT FK1y44v9x6f4ts23ovfpxi0qnqm
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE application_portfolio_urls
    DROP FOREIGN KEY FKrketpvh5st69qrycymhxmvwvh,
    ADD CONSTRAINT FKrketpvh5st69qrycymhxmvwvh
        FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;

ALTER TABLE member_review_tags
    DROP FOREIGN KEY FK9ewk4etp48oj5fl5ue8mw53bg,
    ADD CONSTRAINT FK9ewk4etp48oj5fl5ue8mw53bg
        FOREIGN KEY (member_review_id) REFERENCES member_reviews (id) ON DELETE CASCADE;
